(ns witan.cic.episodes.projected
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [dk.ative.docjure.spreadsheet :as xl]
            [tick.alpha.api :as t]
            [witan.cic.driver.ingest :as i]
            [witan.cic.episodes :as wce]))

(def column-names
  ["Simulation"
   "ID"
   "Episode"
   "Birth Year"
   "Admission Age"
   "Birthday"
   "Start"
   "End"
   "Placement"
   "Offset"
   "Provenance"
   "Placement Sequence"
   "Placement Pathway"
   "Period Start"
   "Period Duration"
   "Period End"
   "Period Offset"
   "Match Offset"
   "Matched ID"
   "Matched Offset"])


(def projected-episode-keys
  [::simulation
   ::id
   ::episode
   ::birth-year
   ::admission-age
   ::birthday
   ::start
   ::end
   ::placement
   ::offset
   ::provenance
   ::placement-sequence
   ::placement-pathway
   ::period-start
   ::period-duration
   ::period-end
   ::period-offset
   ::match-offset
   ::matched-id
   ::matched-offset])

(def mappify-records-xf
  (map #(zipmap projected-episode-keys %)))

(defn csv->data [file-name xf]
  (with-open [reader (io/reader file-name)]
    (into []
          xf
          (csv/read-csv reader))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scrub Record
(defn scrub-record [{::keys [id episode birthday start end placement] :as rec}]
  (let [t (transient rec)]
    (cond-> t
      id        (assoc! ::wce/period-id id)
      episode   (assoc! ::wce/episode-number (i/->int episode))
      start     (assoc! ::wce/report-date (t/date start))
      end       (assoc! ::wce/ceased (t/date end))
      birthday  (assoc! ::wce/birthday (t/date birthday))
      placement (assoc! ::wce/placement placement)
      true      (persistent!))))

(def scrub-records-xf
  (map scrub-record))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Add intervals
(defn add-interval [{::wce/keys [report-date ceased] :as rec}]
  (if (and report-date ceased)
    (assoc rec ::wce/interval (t/new-interval report-date ceased))
    rec))

(def add-interval-xf
  (map add-interval))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Add placement series. Create a sorted map of date to placement for
;; a range of dates at a specific interval (week)
(defn placement-at-date [{::wce/keys [interval placement]} date]
  (if (t/< (t/beginning interval) date (t/end interval))
    [date placement]
    [date nil]))

(defn add-placement-series [report-range episode]
  (assoc episode ::wce/placement-series
         (into (sorted-map)
               (map #(placement-at-date episode %))
               report-range)))

(defn add-placement-series-xf [report-range]
  (map (partial add-placement-series report-range)))

(defn weekly-census [date-range  episodes]
  (let [header (into ["Simulation"
                      "ID" "Episode" "RNE"
                      "Birth Year" "Admission Age" "Birthday"
                      "Episode Start" "Episode End" "Period Start" "Period End"
                      "Provenance" "Placement"]
                     (map str)
                     date-range)]
    (xl/save-workbook!
     "weekly-census.xlsx"
     (xl/create-workbook "Episodes"
                         (into [header]
                               (map (fn [{::wce/keys [period-id birthday reason-new-episode report-date ceased placement placement-series]
                                          ::keys [simulation episode #_birth-year period-start period-end admission-age provenance]}]
                                      (into
                                       [simulation
                                        period-id episode reason-new-episode
                                        (str (t/year birthday)) admission-age (str birthday)
                                        (str report-date) (str ceased) period-start period-end
                                        provenance placement]
                                       (mapv #(if (nil? %) 0 1) (vals placement-series)))))
                               episodes)))))
