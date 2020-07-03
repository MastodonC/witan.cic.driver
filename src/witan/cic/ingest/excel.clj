(ns witan.cic.ingest.excel
  (:require [clojure.string :as string]
            [dk.ative.docjure.spreadsheet :as xl]))

(defn read-row [row]
  (into []
        (comp
         (map (fn [c] (xl/read-cell c)))
         (map (fn [c] (if (string? c) (string/trim c) c))))
        (xl/cell-seq row)))

(defn sheet->rows [sheet]
  (xl/row-seq sheet))

(defn file->sheets [file]
  (into []
        (-> file
            xl/load-workbook
            xl/sheet-seq)))

(defn files->sheets [files]
  (into []
        (mapcat file->sheets)
        files))



