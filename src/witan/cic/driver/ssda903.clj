(ns witan.cic.driver.ssda903
  (:require [tick.alpha.api :as t]))

(defn maybe-date [date]
  (when date
    (t/date date)))

(defn date-of-birth [year-month first-episode-date]
  (let [month-bounds (t/bounds (t/parse year-month))
        end-date (t/min (t/end month-bounds) first-episode-date)]
    (rand-nth (t/range (t/beginning month-bounds) end-date (t/new-duration 1 :days)))))

(defn ceased-after-report-date?
  "Keep open episodes and ones where the report-date is before the ceased"
  [{:keys [report-date ceased]}]
  (if ceased
    (t/< report-date ceased)
    true))

;; A predicate like this would be good
#_(defn birthdate-on-or-before-report-date?
    [{:keys [report_date DOB]}]
    (let [cmp (compare DOB report_date)]
      (or (neg? cmp) (zero? cmp))))

(defn episode-interval [{:keys [report-date ceased] :as rec}]
  (if ceased
    (assoc rec
           :episode-interval
           (t/new-interval report-date ceased))
    rec))
