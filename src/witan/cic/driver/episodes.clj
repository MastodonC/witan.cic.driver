(ns witan.cic.driver.episodes
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(def fields
  [:ID
   :report-date
   :ceased
   :legal-status
   :care-status
   :placement
   :report-year
   :sex
   :DOB
   :UASC])

(defn ->v2-vec-of-vecs [episodes]
  (into [fields]
        (comp
         (map #(-> %
                   (update :report-date str)
                   (update :ceased str)
                   (update :DOB str)))
         (map (apply juxt (map keyword fields))))
        episodes))

(defn ->v2-csv
  "This will output and old style csv that does not have the birth month
  in it. It will currently just strip the year from the DOB."
  [out-file episodes]
  (with-open [w (io/writer out-file)]
    (csv/write-csv w (->v2-vec-of-vecs episodes))))


