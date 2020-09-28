(ns witan.cic.episodes.scrubbed
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [witan.cic.episodes :as wce]))

(defn ->csv [file-name episodes]
  (let [scrubbed-episode-file-keys (first wce/scrubbed-episode-file-header)
        scrubbed-episode-file-header (second wce/scrubbed-episode-file-header)]
    (with-open [writer (io/writer file-name)]
      (csv/write-csv writer
                     (into [scrubbed-episode-file-header]
                           (comp
                            (map-indexed (fn [idx rec] (assoc rec ::wce/row-id (inc idx))))
                            (map (apply juxt scrubbed-episode-file-keys)))
                           episodes)))))
