(ns witan.cic.episodes.transformation-report
  (:require [dk.ative.docjure.spreadsheet :as xl]
            [net.cgrand.xforms :as x]
            [witan.cic.episodes :as wce]
            [witan.cic.ingest.excel :as wcie]))

(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Output transformation report to excel
(defn user-friendly-command-string [cmd]
  (case cmd
    :remove  "for removal"
    :edited  "edited"
    :updated "updated"
    :examine "examine"
    (str cmd)))

(def header
  ["ID" "dob" "sex"
   "report year" "period id"
   ;; "reason for new episode" "report date" "ceased" "placement" "legal status" "care status"
   "RNE" "DECOM" "DEC" "PLACE" "LS" "CIN"
   "file name" "sheet name" "row number"
   "edit status" "reason"
   "affecting report year"
   ;; "new report date" "new ceased" "new placement" "new legal status" "new cares status"
   "DECOM" "DEC" "PLACE" "LS" "CIN"
   "affecting file name" "affecting sheet name" "affecting row number"
   "original fields"
   "DECOM" "DEC" "PLACE" "LS" "CIN"])

(defn format-episode [{::wce/keys [id dob sex
                                   report-year period-id
                                   reason-new-episode report-date ceased placement legal-status care-status]
                       ::wcie/keys [file-name sheet-name row-index]
                       :as episode}]
  [id (str dob) sex
   report-year period-id
   (str reason-new-episode) (str report-date) (str ceased) placement legal-status care-status
   file-name sheet-name row-index])

(defn format-edit [{::wce/keys [command reason] :as edit}
                   {new-report-year ::wce/report-year
                    new-report-date ::wce/report-date
                    new-ceased ::wce/ceased
                    new-placement ::wce/placement
                    new-legal-status ::wce/legal-status
                    new-care-status ::wce/care-status
                    ::wcie/keys [file-name sheet-name row-index]
                    :as new}
                   {previous-report-date ::wce/report-date
                    previous-ceased ::wce/ceased
                    previous-placement ::wce/placement
                    previous-legal-status ::wce/legal-status
                    previous-care-status ::wce/care-status
                    :as previous}]
  [(user-friendly-command-string command) reason
   new-report-year
   (str new-report-date) (str new-ceased) new-placement new-legal-status new-care-status
   file-name sheet-name row-index
   "Original" (str previous-report-date) (str previous-ceased) previous-placement previous-legal-status previous-care-status])

(defn format-replacement [{::wce/keys [command reason]
                           :as edit}
                          {::wce/keys [report-year
                                       report-date ceased placement legal-status care-status]
                           ::wcie/keys [file-name sheet-name row-index]
                           :as replacement}]
  [(user-friendly-command-string command) reason
   report-year
   (str report-date) (str ceased) placement legal-status care-status
   file-name sheet-name row-index])

(defn format-row [episode]
  (let [fields (format-episode episode)
        edit (or
              (some #(when (= (::wce/command %) :remove) %) (::wce/edit episode))
              (first (::wce/edit episode)))]
    (cond
      (::wce/new edit)
      (let [new (::wce/new edit)
            previous (::wce/previous edit)]
        (into fields (format-edit edit new previous)))

      (::wce/replacement edit)
      (let [replacement (::wce/replacement edit)]
        (into fields (format-replacement edit replacement)))

      :else
      (into fields [(user-friendly-command-string (::wce/command edit)) (::wce/reason edit)]))))

(def format-rows-xf
  (comp
   (mapcat (fn [[_ {::wce/keys [ssda903-episodes]}]] ssda903-episodes))
   (x/sort-by (juxt ::wce/id ::wce/report-date ::wce/report-year))
   (map format-row)))

(defn ->excel [out-file episodes]
  (let [template (xl/load-workbook "template-transformation-report.xlsx")
        sheet (xl/select-sheet "Transformation Report" template)]
    (xl/add-rows! sheet
                  (x/into []
                          format-rows-xf
                          episodes))
    (xl/save-workbook! out-file template)))

(comment

  ;; old way to save ->excel
  #_(xl/save-workbook!
     out-file
     (xl/create-workbook
      "Transformation Report"
      (x/into [header]
              format-rows-xf
              episodes)))

  )
