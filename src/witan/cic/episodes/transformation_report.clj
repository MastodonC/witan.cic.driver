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
    cmd))

(defn ->excel [out-file episodes]
  (xl/save-workbook!
   out-file
   (xl/create-workbook
    "Transformation Report"
    (x/into [["ID" "dob" "sex"
              "report year"
              ;; "reason for new episode" "report date" "ceased" "placement" "legal status" "care status"
              "RNE" "DECOM" "DEC" "PLACE" "LS" "CIN"
              "file name" "sheet name" "row number"
              "edit status" "reason"
              "affecting report year"
              ;; "new report date" "new ceased" "new placement" "new legal status" "new cares status"
              "DECOM" "DEC" "PLACE" "LS" "CIN"
              "affecting file name" "affecting sheet name" "affecting row number"
              "original fields"
              "DECOM" "DEC" "PLACE" "LS" "CIN"]]
            (comp
             (mapcat (fn [[_ {::wce/keys [ssda903-episodes]}]] ssda903-episodes))
             (x/sort-by (juxt ::wce/id ::wce/report-date ::wce/report-year))
             (map (fn [episode]
                    (let [fields ((juxt ::wce/id (fn [e] (str (::wce/dob e))) ::wce/sex
                                        ::wce/report-year
                                        ::wce/rne (fn [e] (str (::wce/report-date e))) (fn [e] (str (::wce/ceased e))) ::wce/placement ::wce/legal-status ::wce/care-status
                                        ::wcie/file-name ::wcie/sheet-name ::wcie/row-index)
                                  episode)
                          edit (or
                                (some #(when (= (::wce/command %) :remove) %) (::wce/edit episode))
                                (first (::wce/edit episode)))]
                      (cond
                        (::wce/new edit)
                        (let [new (::wce/new edit)
                              previous (::wce/previous edit)]
                          (into fields
                                [(user-friendly-command-string (::wce/command edit)) (::wce/reason edit)
                                 (::wce/report-year new)
                                 (str (::wce/report-date new)) (str (::wce/ceased new)) (::wce/placement new) (::wce/legal-status new) (::wce/care-status new)
                                 (::wcie/file-name new) (::wcie/sheet-name new) (::wcie/row-index new)
                                 "Original" (str (::wce/report-date previous)) (str (::wce/ceased previous)) (::wce/placement previous) (::wce/legal-status previous) (::wce/care-status previous)]))

                        (::wce/replacement edit)
                        (let [replacement (::wce/replacement edit)]
                          (into fields [(user-friendly-command-string (::wce/command edit)) (::wce/reason edit)
                                        (::wce/report-year replacement)
                                        (str (::wce/report-date replacement)) (str (::wce/ceased replacement)) (::wce/placement replacement) (::wce/legal-status replacement) (::wce/care-status replacement)
                                        (::wcie/file-name replacement) (::wcie/sheet-name replacement) (::wcie/row-index replacement)]))

                        :else
                        (into fields [(user-friendly-command-string (::wce/command edit)) (::wce/reason edit)]))))))
            episodes))))
