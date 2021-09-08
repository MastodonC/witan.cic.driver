(ns witan.cic.ingest.excel
  (:require [clojure.string :as string]
            [dk.ative.docjure.spreadsheet :as xl])
  (:import [org.apache.poi.ss.usermodel Row]))

(defn read-row [row]
  (into []
        (comp
         (map (fn [c] (xl/read-cell c)))
         (map (fn [c] (if (string? c) (string/trim c) c))))
        (xl/cell-seq row)))

(defn sheet->rows [sheet]
  (xl/row-seq sheet))

(defn file->sheets [file-name]
  (into []
        (-> file-name
            xl/load-workbook
            xl/sheet-seq)))

(defn files->sheets [files]
  (into []
        (mapcat file->sheets)
        files))

(def file-names->workbook-xf
  (map (fn [file-name] {::file-name file-name
                        ::workbook (xl/load-workbook file-name)})))

(def workbook->data-xf
  (comp
   (map (fn [{:keys [::file-name ::workbook]}]
          {::file-name file-name
           ::sheets (xl/sheet-seq workbook)}))
   (mapcat (fn [{:keys [::file-name ::sheets]}]
             (into []
                   (comp
                    (map (fn [sheet]
                           {::file-name file-name
                            ::sheet-name (xl/sheet-name sheet)
                            ::rows (into [] (xl/row-seq sheet))}))
                    (mapcat (fn [{:keys [::file-name ::sheet-name ::rows]}]
                              (try (into []
                                         (comp
                                          (keep identity)
                                          (map (fn [^Row row]
                                                 {::file-name file-name
                                                  ::sheet-name sheet-name
                                                  ::row row
                                                  ::row-index (inc (.getRowNum row))
                                                  ::cells (read-row row)}))
                                          (filter #(some some? (::cells %))))
                                         rows)
                                   (catch Exception e (throw (ex-info "Failed to extract data from row"
                                                                      {:file-name file-name
                                                                       :sheet-name sheet-name
                                                                       :rows rows}
                                                                      e)))))))
                   sheets)))))

(def files->data-xf
  (comp
   file-names->workbook-xf
   workbook->data-xf))

(defn files->data [file-names]
  (into []
        files->data-xf
        file-names))
