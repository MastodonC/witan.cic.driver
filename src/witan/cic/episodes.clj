(ns witan.cic.episodes
  (:require [clojure.spec.alpha :as spec]
            [net.cgrand.xforms :as x]
            [tick.alpha.api :as t]))

;; FIXME: shorten file-name to be basename w/o the path.

(set! *warn-on-reflection* true)

;; Define the strucutre for the episodes map

;; p19 of SSDA903 Guide: "Episodes of care are “building blocks” for a
;; period of care, which is defined as a period during which a child
;; is continuously looked after by a local authority."

(def key-names
  {::id "ID"
   ::report-date "Report Date"
   ::ceased "Ceased"
   ::reason-new-episode "Reason New Episode"
   ::legal-status "Legal Status"
   ::care-status "Care Status"
   ::placement "Placement"})

(def scrubbed-episode-file-header
  [[::row-id ::id ::reason-new-episode ::report-date ::ceased ::legal-status ::care-status ::placement ::report-year ::sex ::dob ::uasc ::period-id ::episode-number ::phase-number ::phase-id]
   [""        "ID" "RNE"               "report_date" "ceased" "legal_status" "care_status" "placement" "report_year" "sex" "DOB" "UASC" "period_id" "episode_number" "phase_number" "phase_id"]])

(def output-simulations-header
  [[::simulation ::id ::episode-id]
   ["Simulation" "ID" "Episode" "Birth Year" "Admission Age" "Birthday" "Start" "End" "Placement"]])

(def reason-new-episode-of-care-lookup
  {"S" "Started to be looked after"
   "L" "Change of legal status only"
   "P" "Change of placement and carer(s) only"
   "T" "Change of placement (but same carer(s)) only"
   "B" "Change of legal status and placement and carer(s) at the same time"
   "U" "Change of legal status and change of placement (but same carer(s)) at the same time"})

(def legal-status-lookup
  {"C1" "Interim care order"
   "C2" "Full care order"
   "D1" "Freeing order granted"
   "E1" "Placement order granted"
   "V2" "Single period of accommodation under section 20 (Children Act 1989)"
   "V3" "Accommodated under an agreed series of short-term breaks, when individual episodes of care are recorded"
   "V4" "Accommodated under an agreed series of short-term breaks, when agreements are recorded (NOT individual episodes of care)"
   "L1" "Under police protection and in local authority accommodation"
   "L2" "Emergency protection order (EPO)"
   "L3" "Under child assessment order and in local authority accommodation"
   "J1" "Remanded to local authority accommodation or to youth detention accommodation"
   "J2" "Placed in local authority accommodation under the Police and Criminal Evidence Act 1984, including secure accommodation. However, this would not necessarily be accommodation where the child would be detained."
   "J3" "Sentenced to Youth Rehabilitation Order (Criminal Justice and Immigration Act 2008 , as amended by Legal Aid, Sentencing and Punishment of Offenders Act (LASPOA) 2012, with residence or intensive fostering requirement)"})

(def category-of-need-lookup
  {"N1" "Abuse or neglect"
   "N2" "Child’s disability"
   "N3" "Parental illness or disability"
   "N4" "Family in acute stress"
   "N5" "Family dysfunction"
   "N6" "Socially unacceptable behaviour"
   "N7" "Low income"
   "N8" "Absent parenting"})

(def category-of-need-long-lookup
  {"N1" "Child in need as a result of, or at risk of, abuse or neglect"
   "N2" "Child and their family whose main need for children’s social care services arises out of the child’s disabilities, illness or intrinsic condition"
   "N3" "Child whose main need for children’s social care services arises because the capacity of their parent(s) or carer(s) to care for them is impaired by disability, illness, mental illness, or addictions"
   "N4" "Child whose needs arise from living in a family going through a temporary crisis such that parenting capacity is diminished and some of the children’s needs are not being adequately met"
   "N5" "Child whose needs arise mainly out of their living with a family where the parenting capacity is chronically inadequate" "N6" "Child and family whose need for children’s social care services arises primarily out of their behaviour impacting detrimentally on the community"
   "N7" "Child, either living in a family or independently, whose need for children’s social care services arises mainly from being dependent on an income below the standard state entitlements"
   "N8" "Child whose need for children’s social care services arises mainly from having no parent(s) available to provide for them. A child whose parent(s) decide it is in the best interest for the child to be adopted would be included in this category"})

(def placement-lookup
  {"A3" "Placed for adoption with parental/guardian consent with current foster carer(s) (under Section 19 of the Adoption and Children Act 2002) or with a freeing order where parental/guardian consent has been given (under Section 18(1)(a) of the Adoption Act 1976)"
   "A4" "Placed for adoption with parental/guardian consent not with current foster carer(s) (under Section 19 of the Adoption and Children Act 2002) or with a freeing order where parental/guardian consent has been given (under Section 18(1)(a) of the Adoption Act 1976)"
   "A5" "Placed for adoption with placement order with current foster carer(s) (under Section 21 of the Adoption and Children Act 2002) or with a freeing order where parental/guardian consent was dispensed with (under Section 18(1)(b) the Adoption Act 1976)"
   "A6" "Placed for adoption with placement order not with current foster carer(s) (under Section 21 of the Adoption and Children Act 2002) or with a freeing order where parental/guardian consent was dispensed with (under Section 18(1)(b) of the Adoption Act 1976)"
   "H5" "Semi-independent living accommodation not subject to children’s homes regulations"
   "K1" "Secure children’s homes"
   "K2" "Children’s Homes subject to Children’s Homes Regulations"
   "P1" "Placed with own parent(s) or other person(s) with parental responsibility"
   "P2" "Independent living for example, in a flat, lodgings, bedsit, bed and breakfast (B&B) or with friends, with or without formal support"
   "P3" "Residential employment"
   "R1" "Residential care home"
   "R2" "National Health Service (NHS)/health trust or other establishment providing medical or nursing care"
   "R3" "Family centre or mother and baby unit"
   "R5" "Young offender institution (YOI) or prison"
   "S1" "All residential schools, except where dual-registered as a school and children’s home"
   "T0" "All types of temporary move (see paragraphs above for further details)"
   "T1" "Temporary periods in hospital"
   "T2" "Temporary absences of the child on holiday"
   "T3" "Temporary accommodation whilst normal foster carer(s) is/are on holiday"
   "T4" "Temporary accommodation of seven days or less, for any reason, not covered by codes T1 to T3"
   "U1" "Foster placement with relative(s) or friend(s) – long term fostering"
   "U2" "Fostering placement with relative(s) or friend(s) who is/are also an approved adopter(s) – fostering for adoption/concurrent planning"
   "U3" "Fostering placement with relative(s) or friend(s) who is/are not long-term or fostering for adoption /concurrent planning"
   "U4" "Foster placement with other foster carer(s) – long term fostering"
   "U5" "Foster placement with other foster carer(s) who is/are also an approved adopter(s) – fostering for adoption /concurrent planning"
   "U6" "Foster placement with other foster carer(s) – not long term or fostering for adoption /concurrent planning"
   "Z1" "Other placements (must be listed on a schedule sent to DfE with annual submission)"
   "Q1" "Foster placement with relative or friend - now U1-U3"
   "Q2" "Foster placement with other foster carer - now U4-U6"
   "M1" "Missing - discontinued"
   "M2" "Missing - discontinued"
   "M3" "Missing - discontinued"})

(spec/def ::simulation-id int?)
(spec/def ::episode-id string?) ;; ::id ::episode-number
(spec/def ::id int)
(spec/def ::dob (spec/and string? #(re-matches #"[1-2][0-9][0-9][0-9]-[0-1][0-9]" %))) ;; YYYY-MM
(spec/def ::reason-new-episode (into #{} (keys reason-new-episode-of-care-lookup)))
(spec/def ::report-date inst?)
(spec/def ::report-year int?) ;; YYYY
(spec/def ::sex #{1 2})
(spec/def ::uasc boolean?)
(spec/def ::ceased (spec/nilable inst?))
(spec/def ::legal-status (into #{} (keys legal-status-lookup)))
(spec/def ::care-status (into #{} (keys category-of-need-lookup)))
(spec/def ::placement (into #{} (keys placement-lookup)))


(spec/def ::period-id (spec/and string? #(re-matches #"[0-9]+-[0-9]+" %))) ;; ID-<index of period of continous-care> We can break each child's episodes into periods of continuous care
(spec/def ::episode-number int?) ;; <index of episode within period of care> After assigning period ID, we can assign contiguous episode numbers within each period
(spec/def ::phase-number int?) ;; <index phase of same-placement care within each period>
(spec/def ::phase-id (spec/and string? #(re-matches #"[0-9]+-[0-9]+-[0-9]+" %))) ;; <period-id>-<phase-number> After assigning period ID, we can assign phases of same-placement care within each period

(spec/def ::ssda903-header-record
  (spec/keys :req [::id ::sex ::dob]))

(spec/def ::sda903-episode-record
  (spec/keys :req [::id ::report-date ::ceased ::care-status ::legal-status ::reason-new-episode ::placement]))

(spec/def ::episode-enriched-record
  (spec/keys :req [::id ::reason-new-episode ::report-date ::ceased ::legal-status ::care-status ::placement ::report-year ::sex ::dob ::uasc]))

(spec/def ::episode-scrubbed-record
  (spec/keys :req [::id ::reason-new-episode ::report-date ::ceased ::legal-status ::care-status ::placement ::report-year ::sex ::dob ::uasc ::period-id ::episode-number ::phase-number ::phase-id]))

(spec/def ::episode-output-record
  (spec/keys :req [::simulation-id ::id ::episode-number ::birth-year ::admission-age ::birthday ::start ::end ::placement]))


(defn episode-record? [ssda903-record]
  (and (contains? ssda903-record ::report-date)
       (contains? ssda903-record ::placement)))

(defn header-record? [ssda903-record]
  (and (contains? ssda903-record ::sex)
       (contains? ssda903-record ::dob)))

(defn tagged-for-removal? [tagged-episode]
  ((into #{}
         (map ::command)
         (get tagged-episode ::edit)) :remove))

(defn tagged-as-edited? [tagged-episode]
  ((into #{}
         (map ::command)
         (get tagged-episode ::edit)) :edited))

(defn tagged-for-examination? [tagged-episode]
  ((into #{}
         (map ::command)
         (get tagged-episode ::edit)) :examine))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Episode intervals
;; Given an episode
;; When the ceased date is before the report-date
;; Then mark the episode for removal
(defn bad-episode-interval?
  "Only when a non-nil cease date happens before a report date"
  [{::keys [report-date ceased]}]
  (and ceased
       (t/< ceased report-date)))

(def good-episode-interval? (complement bad-episode-interval?))

(defn mark-bad-episode-interval [episode]
  (if (bad-episode-interval? episode)
    (update episode ::edit (fnil conj []) {::command :remove ::reason "Episode ceases before it starts"})
    episode))

(def mark-bad-episode-interval-xf
  (map mark-bad-episode-interval))

(defn add-episode-interval [{::keys [report-date ceased] :as episode}]
  (if (and ceased
           (good-episode-interval? episode))
    (assoc episode ::interval (t/new-interval (t/at report-date "13:00")
                                              (t/at ceased "13:00")))
    episode))

(def add-episode-interval-xf
  (map add-episode-interval))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; remove legal status v3 v4 as they are short term
;; Given an episode
;; When the legal status is V3 of V4 for short term breaks
;; Then mark the episode for removal
(def short-term-legal-break-status-codes #{"V3" "V4"})

(defn short-term-break-legal-status? [{::keys [legal-status]}]
  (short-term-legal-break-status-codes legal-status))

(defn mark-short-term-break-legal-status-episodes [{::keys [legal-status] :as episode}]
  (if (short-term-break-legal-status? episode)
    (update episode ::edit (fnil conj []) {::command :remove
                                           ::reason (format "Legal status for episode is %s which is %s"
                                                            legal-status
                                                            (legal-status-lookup legal-status))})
    episode))

(def mark-short-term-break-legal-status-episodes-xf
  (map mark-short-term-break-legal-status-episodes))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Map U* -> Q*
;; - "Q1" "Foster placement with relative or friend - now U1-U3"
;; - "Q2" "Foster placement with other foster carer - now U4-U6"
;;
;; Question: Should we mark these as "edited"? We'd need a new
;; category to distinguish them from the editing start/end dates on
;; episodes later. I'm going to go for updated for now.
(def q1-codes #{"U1" "U2" "U3"})

(def q2-codes #{"U4" "U5" "U6"})

(defn mark-fostering-as-updated [{::keys [placement] :as episode}]
  (cond
    (q1-codes placement) (-> episode
                             (assoc ::placement "Q1")
                             (update ::edit (fnil conj []) {::command :updated
                                                            ::reason  (format "Placement %s mapped to Q1 for historical comparison." placement)}))
    (q2-codes placement) (-> episode
                             (assoc ::placement "Q2")
                             (update ::edit (fnil conj []) {::command :updated
                                                            ::reason  (format "Placement %s mapped to Q2 for historical comparison." placement)}))
    :else episode))

(def mark-fostering-as-updated-xf
  (map mark-fostering-as-updated))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Episode required fields: id, report-date, report-year, placement, legal-status, care-status
;; Given an episode
;; When the ID, report-date, report-year, placement, legal-status, or care-status are missing
;; Then mark the episode for removal
(defn mark-missing-required-episode-fields [rec]
  (if (episode-record? rec)
    (cond-> rec
      (nil? (::id rec))
      (update ::edit (fnil conj [])
              {::command :remove
               ::reason "Missing id field."})

      (nil? (::report-date rec))
      (update ::edit (fnil conj [])
              {::command :remove
               ::reason "Missing report-date field."})

      (nil? (::report-year rec))
      (update ::edit (fnil conj [])
              {::command :remove
               ::reason "Missing report-year field."})

      (nil? (::placement rec))
      (update ::edit (fnil conj [])
              {::command :remove
               ::reason "Missing placement field."})

      (nil? (::legal-status rec))
      (update ::edit (fnil conj [])
              {::command :remove
               ::reason "Missing legal-status field."})

      (nil? (::care-status rec))
      (update ::edit (fnil conj [])
              {::command :remove
               ::reason "Missing care-status field."}))
    rec))

(def mark-missing-required-episode-fields-xf
  (map mark-missing-required-episode-fields))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Header required fields: id, dob, sex
;; Given a header record
;; When the id, dob, or sex fields are missing
;; Then mark the header record for removal
(defn mark-missing-required-header-fields [rec]
  (if (header-record? rec)
    (cond-> rec
      (nil? (::id rec))
      (update ::edit (fnil conj [])
              {::command :remove
               ::reason "Missing id field."})

      (nil? (::dob rec))
      (update ::edit (fnil conj [])
              {::command :remove
               ::reason "Missing dob field."})

      (nil? (::sex rec))
      (update ::edit (fnil conj [])
              {::command :remove
               ::reason "Missing sex field."}))
    rec))

(def mark-missing-required-header-fields-xf
  (map mark-missing-required-header-fields))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Group headers and episodes by id
(defn ssda903-split-headers-and-episodes
  ([] {::ssda903-headers []
       ::ssda903-episodes []})
  ([rec]
   rec)
  ([acc ssda903-record]
   (cond
     (header-record? ssda903-record)
     (update acc ::ssda903-headers conj ssda903-record)

     (episode-record? ssda903-record)
     (update acc ::ssda903-episodes conj ssda903-record)

     :else
     (throw (ex-info "Record is neither a header nor an episode" {:data ssda903-record})))))

(def headers-and-episodes-by-id-xf
  (x/by-key ::id
            (x/reduce ssda903-split-headers-and-episodes)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; remove placement m1, m2, m3 as they are discontinued.
;;
;; If the report_date of e2 for a M* episode is the same as the ceased
;; of e1, then the ceased of e1 should be the ceased of e2 and e2
;; should be marked for removal
;;
;; Given an episode
;; When the placment code is the discontinued M1, M2, or M3
;; Then mark the episode for removal
;;
;; Assumption: CYP are not missing at the beginning of a period but
;; only at the end or middle of a period of care
(def discontinued-missing-placement-codes #{"M1" "M2" "M3"})

(defn discontinued-missing-placement? [{::keys [placement]}]
  (discontinued-missing-placement-codes placement))

(defn mark-fixed-missing-placement-episodes-for-id-rf
  ([] {:marked-for-removal-or-edited []
       :episodes []})
  ([acc] (x/into (:marked-for-removal-or-edited acc) (:episodes acc)))
  ([{:keys [episodes] :as acc} new]
   (try
     (let [previous (peek episodes)]
       (if (or (tagged-as-edited? new)
               (tagged-for-removal? new))
         (update acc :marked-for-removal-or-edited conj new)
         (if (discontinued-missing-placement? new)
           (-> acc
               (assoc :episodes
                      (-> (pop episodes)
                          (conj (->  previous
                                     ;; log what we've done
                                     (update
                                      ::edit
                                      (fnil conj [])
                                      {::command :edited
                                       ::reason "Ceased date of episode updated to be ceased date of following M* episode"
                                       ::desciption (format "Changing cease date from %s to %s" (::ceased previous) (::ceased new))
                                       ::previous previous
                                       ::new new})
                                     ;; update the ceased
                                     (assoc ::ceased (::ceased new))
                                     ;; And then update the interval
                                     (add-episode-interval)))))
               (update :marked-for-removal-or-edited
                       (fnil conj [])
                       (update new
                               ::edit
                               (fnil conj [])
                               {::command :remove
                                ::reason "Episode is a discontinued M* placement"
                                ::desciption "Ceased date of previous episode changed to cease date of this one."
                                ::previous previous
                                ::new new})))
           (update acc :episodes conj new))))
     (catch Exception e
       (throw (ex-info "Couldn't handle new episode." {:new new :acc acc} e))))))

(defn mark-fixed-missing-placement-episodes-for-id [[id {::keys [ssda903-episodes] :as rec}]]
  [id
   (assoc rec ::ssda903-episodes
          (transduce
           (x/sort-by (juxt ::report-year ::report-date))
           mark-fixed-missing-placement-episodes-for-id-rf
           ssda903-episodes))])

(def mark-fixed-missing-placement-episodes-xf
  (map mark-fixed-missing-placement-episodes-for-id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stale Episodes
;; Given episode with a nil cease date (a)
;; When there is a more recently reported episode with the same start date (b)
;; Then mark episode (a) for removal
;;
;; If the report_date of e1 is before the report_date of e2 then the
;; report_date of e2 should become the ceased of e1
(defn mark-stale-episodes-for-id-rf
  "Only the last episode should have a ::ceased of nil"
  ([] [])
  ([acc] acc)
  ([acc new]
   (let [previous (peek acc)]
     (cond
       ;; if previous ceased is nil and report_dates are the same or the latest is earlier then keep latest
       (and previous
            (nil? (::ceased previous))
            (t/<= (::report-date new) (::report-date previous)))
       (let [previous' (update previous ::edit (fnil conj []) {::command :remove
                                                               ::reason "Open episode superseded by more recent record"
                                                               ::replacement new})]
         (-> (pop acc)
             (conj previous' new)))
       ;; If the report date of the new is after the report date of the previous unclosed episode then close the previous episode
       (and previous
            (nil? (::ceased previous))
            (t/< (::report-date previous) (::report-date new)))
       (let [previous' (-> previous
                           (assoc ::ceased (::report-date new))
                           (add-episode-interval)
                           (update ::edit (fnil conj []) {::command :edited
                                                          ::reason "Open episode with nil cease date closed by report_date of later epiosde."
                                                          ::previous previous
                                                          ::new new}))]
         (-> (pop acc)
             (conj previous' new)))
       :else
       (conj acc new)))))

(defn mark-stale-episodes-for-id [[id {::keys [ssda903-episodes] :as rec}]]
  [id
   (assoc rec ::ssda903-episodes
          (transduce
           (x/sort-by (juxt ::report-year ::report-date))
           mark-stale-episodes-for-id-rf
           ssda903-episodes))])

(def mark-stale-episodes-xf
  "This depends on being passed a map of id {::ssda903-episodes}"
  (map mark-stale-episodes-for-id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Exclude entire stale histories
;;
;; If the last episode is open and not in the max report-year then the
;; entire history should be marked for removal
(defn mark-stale-history [max-report-year [id {::keys [ssda903-episodes] :as rec}]]
  (try
    (let [last-episode (peek (into []
                                   (comp
                                    (remove tagged-for-removal?)
                                    (x/sort-by (juxt ::report-year ::report-date)))
                                   ssda903-episodes))]
      (if (and
           last-episode
           (nil? (::ceased last-episode))
           (< (::report-year last-episode) max-report-year))
        [id
         (assoc rec
                ::ssda903-episodes
                (into []
                      (map (fn [episode]
                             (update episode ::edit (fnil conj [])
                                     {::command :remove
                                      ::reason "Last episode is open, but not reported in last report-year"})))
                      ssda903-episodes))]
        [id rec]))
    (catch Exception e
      (throw (ex-info "Failed to process history for staleness."
                      {:max-report-year max-report-year
                       :id id
                       :rec rec}
                      e))) ))

(defn mark-stale-history-xf [max-report-year]
  (map (partial mark-stale-history max-report-year)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Overlapping Episodes
;;
;; Given an epiosde (a) with a report-date and cease date
;; When there is an episode (b) with a later report-year and the same report-date
;; Mark episode (a) for removal
;;
;; Given an episode (a) with a report-date and cease date
;; When there is an episode (b) that contains episode (a) completely
;; Then mark episode (a) for removal
;;
;; Given an episode (a) with a report-date and a cease date
;; When there is a closed episode (b) with a more recent report-year and a report-date that is ealier than the cease date of episode (a)
;; Then update the ceased date of episode (a) to be the report-date of episode (b)
(defn same-report-date-later-report-year? [previous new]
  (and (= (::report-date previous) (::report-date new))
       (< (::report-year previous) (::report-year new))))

(defn previous-during-new?
  "Does the previous episode happen entirely during the new episode. This will be true if the relation is :during or :finishes"
  [previous new]
  (when (and (::interval previous)
             (::interval new))
    (#{:during :finishes}
     (t/relation (::interval previous) (::interval new)))))

(defn previous-overlaps-new? [previous new]
  (when (and (::interval previous)
             (::interval new))
    (= :overlaps
       (t/relation (::interval previous) (::interval new)))))

(defn update-previous-episode [new previous]
  (cond
    (same-report-date-later-report-year? previous new)
    (update previous ::edit (fnil conj []) {::command :remove
                                            ::reason "Closed episode superseded by more recent record"
                                            ::replacement new})

    (previous-during-new? previous new)
    (update previous ::edit (fnil conj []) {::command :remove
                                            ::reason (format "Superseded by more recent record which %s"
                                                             (case (previous-during-new? previous new)
                                                               :during "contiains this one."
                                                               :finishes "finishes this one."))
                                            ::replacement new})

    (previous-overlaps-new? previous new)
    (-> previous
        (update ::edit (fnil conj []) {::command :edited
                                       ::reason "Overlaps with more recent record. Changing cease date."
                                       ::description (format "Changing cease date from %s to %s to be report-date of more recent record." (::ceased previous) (::report-date new))
                                       ::previous previous
                                       ::new new})
        (assoc ::ceased (::report-date new))
        (assoc ::interval (t/new-interval (t/at (::report-date previous) "13:00")
                                          (t/at (::report-date new) "13:00"))))
    :else
    previous))

(defn update-existing-episodes [previous new]
  (x/into []
          (map (partial update-previous-episode new))
          previous))

(defn clashing-intervals? [previous new]
  (when (and (::interval previous)
             (::interval new))
    (#{:overlapped-by :starts :started-by :during :finished-by}
     (t/relation (::interval previous) (::interval new)))))

(defn finished-by? [previous new]
  (when (and (::interval previous)
             (::interval new))
    (=  :finished-by
        (t/relation (::interval previous) (::interval new)))))

(defn update-new-episode [acc new]
  (let [previous (-> acc :episodes peek)]
    (cond
      (finished-by? previous new)
      (update acc :marked-for-removal conj
              (update new ::edit (fnil conj []) {::command :remove
                                                 ::reason (format "Previous record %s new record."
                                                                  (t/relation (::interval previous) (::interval new)))
                                                 ::conflicting-record previous}))

      (clashing-intervals? previous new)
      (update acc :episodes conj
              (update new ::edit (fnil conj []) {::command :examine
                                                 ::reason (format "Previous record %s new record."
                                                                  (t/relation (::interval previous) (::interval new)))
                                                 ::conflicting-record previous}))

      :else
      (update acc :episodes conj new))))

(defn mark-overlapping-episodes-for-id-rf
  ([] {:marked-for-removal []
       :episodes []})
  ([acc] (x/into (:marked-for-removal acc) (:episodes acc)))
  ([{:keys [episodes] :as acc} new]
   (let [previous (peek episodes)]
     (try
       (if (tagged-for-removal? new)
         (update acc :marked-for-removal conj new)
         (-> acc
             (assoc :episodes (update-existing-episodes episodes new))
             (update-new-episode new)))
       (catch Exception e
         (throw (ex-info "Couldn't handle new episode." {:new new :acc acc :previous previous} e)))))))

(defn mark-overlapping-episodes-for-id [[id {::keys [ssda903-episodes] :as rec}]]
  [id
   (assoc rec ::ssda903-episodes
          (transduce
           (x/sort-by (juxt ::report-year ::report-date))
           mark-overlapping-episodes-for-id-rf
           ssda903-episodes))])

(def mark-overlapping-episodes-for-id-xf
  (map mark-overlapping-episodes-for-id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; unfinished episodes that occur during earlier episodes
;;
;; Given an episode (a) with a report-date and a cease date
;; When there is an open episode (b) with a more recent report-year and a report-date that is ealier than the cease date of episode (a)
;; Then update the ceased date of episode (a) to be the report-date of episode (b)
(defn update-episode-overlapped-by-open-episode
  [previous new]
  (try
    (if (and
         (nil? (::interval new))
         (t/< (::report-date new) (::ceased previous)))
      (-> previous
          (assoc ::ceased (::report-date new)
                 ::interval (t/new-interval (t/beginning (::interval previous))
                                            (t/at (::report-date new) "13:00")))
          (update ::edit (fnil conj []) {::command :edited
                                         ::reason "Report date of more recent open episode before ceased date of previous. Changing cease date."
                                         ::desciption (format "Changing cease date from %s to %s" (::ceased previous) (::report-date new))
                                         ::previous previous
                                         ::new new}))
      previous)
    (catch Exception e
      (throw (ex-info "Failed to updated overlapped by open episode." {:previous previous :new new} e)))))

(defn mark-episode-overlapped-by-open-episodes-rf
  ([] {:marked-for-removal-or-edited []
       :episodes []})
  ([acc] (x/into (:marked-for-removal-or-edited acc) (:episodes acc)))
  ([{:keys [episodes] :as acc} new]
   (try
     (let [idx (dec (count episodes))]
       (if (or (tagged-as-edited? new)
               (tagged-for-removal? new))
         (update acc :marked-for-removal-or-edited conj new)
         (if (and
              (< -1 idx)
              (nil? (::interval new)))
           (-> acc
               (assoc-in [:episodes idx] (update-episode-overlapped-by-open-episode (get-in acc [:episodes idx]) new))
               (update :episodes conj new))
           (update acc :episodes conj new))))
     (catch Exception e
       (throw (ex-info "Couldn't handle new episode." {:new new :acc acc} e))))))


(defn mark-episode-overlapped-by-open-episodes [[id {::keys [ssda903-episodes] :as rec}]]
  [id
   (assoc rec ::ssda903-episodes
          (transduce
           (x/sort-by (juxt ::report-year ::report-date))
           mark-episode-overlapped-by-open-episodes-rf
           ssda903-episodes))])

(def mark-episode-overlapped-by-open-episodes-xf
  (map mark-episode-overlapped-by-open-episodes))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Properties of episodes per CYP after processing
;;
;; 1. No episode should overlap any other
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ordered-disjoint-episodes? [[id {::keys [ssda903-episodes] :as rec}]]
  [id
   (assoc rec ::ordered-disjoint-intervals
          (t/ordered-disjoint-intervals?
           (x/into []
                   (comp
                    (remove tagged-for-removal?)
                    (x/sort-by ::report-date)
                    (map (fn [{::keys [interval report-date]}]
                           (or interval (t/new-interval (t/at report-date "13:00")
                                                        (t/+ (t/at report-date "13:00") (t/new-duration 1 :days)))))))
                   ssda903-episodes)))])


(def mark-ordered-disjoint-episodes-xf
  (map ordered-disjoint-episodes?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Create Header
(defn create-header [[id {::keys [ssda903-headers] :as rec}]]
  [id
   (assoc rec ::header (reduce merge ssda903-headers))])

(def create-header-xf
  (map create-header))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Separate episodes for removal for further processing
(defn separate-episodes-for-removal [[id {::keys [ssda903-episodes] :as rec}]]
  (let [episodes         (x/into []
                                 (remove tagged-for-removal?)
                                 ssda903-episodes)
        removed-episodes (x/into []
                                 (filter tagged-for-removal?)
                                 ssda903-episodes)]
    [id
     (assoc rec
            ::episodes episodes
            ::removed-episodes removed-episodes)]))

(def separate-episodes-for-removal-xf
  (map separate-episodes-for-removal))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Add header fields to episodes
(defn add-header-fields-to-episode [header episode]
  (merge episode (select-keys header [::dob ::sex])))

(defn add-header-fields-to-episodes [[id {::keys [ssda903-episodes header] :as rec}]]
  [id
   (assoc rec
          ::ssda903-episodes
          (x/into []
                  (map (fn [episode] (add-header-fields-to-episode header episode)))
                  ssda903-episodes))])

(def add-header-fields-to-episodes-xf
  (map add-header-fields-to-episodes))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mark UASC records for removal
(defn mark-uasc-for-removal [uasc-ids record]
  (if (and (episode-record? record)
           (uasc-ids (::id record)))
    (-> record
        (update ::edit (fnil conj []) {::command :remove
                                       ::reason "Record is about an UASC"})
        (assoc ::uasc true))
    (assoc record ::uasc false)))

(defn mark-uasc-for-removal-xf [uasc-ids]
  (map (partial mark-uasc-for-removal uasc-ids)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Create periods of care
;;
;; A period is made up of episodes where there is no gap between the
;; previous cease date and the next report-date.
;;
;; A phase is when 2 episodes in the same period have the same
;; placement type.
(defn period-number-and-episode-number [new previous]
  (let [previous-interval (::interval previous)
        new-interval (or (::interval new)
                         (t/new-interval (t/at (::report-date new) "13:00")
                                         (t/at (::report-date new) "23:59")))]
    (if (and previous-interval
             (= :meets (t/relation previous-interval new-interval)))
      (assoc new
             ::episode-number (inc (::episode-number previous))
             ::period-number (::period-number previous))
      (assoc new
             ::episode-number 1
             ::period-number (if-let [period-number (::period-number previous)]
                               (inc period-number)
                               1)))))

(defn period-id [episode]
  (assoc episode ::period-id (format "%s-%s"
                                     (::id episode)
                                     (::period-number episode))))

(defn phase-number [new previous]
  (cond
    ;; same placement, same period, same phase
    (and previous
         (= (::placement previous) (::placement new))
         (= (::period-number previous) (::period-number new)))
    (assoc new ::phase-number (::phase-number previous))

    ;; different placement, same period, old phase + 1
    (and previous
         (not (= (::placement previous) (::placement new)))
         (= (::period-number previous) (::period-number new)))
    (assoc new ::phase-number (inc (::phase-number previous)))

    ;; same placement, new period, phase = 1
    (and previous
         (= (::placement previous) (::placement new))
         (< (::period-number previous) (::period-number new)))
    (assoc new ::phase-number 1)

    ;; no previous period phase = 1
    :else
    (assoc new ::phase-number 1)))

(defn phase-id [episode]
  (assoc episode ::phase-id (format "%s-%s"
                                    (::period-id episode)
                                    (::phase-number episode))))

(defn add-periods-of-care-rf
  ([] {:marked-for-removal []
       :episodes []})
  ([acc] (x/into (:marked-for-removal acc) (:episodes acc)))
  ([acc new]
   (if (tagged-for-removal? new)
     (update acc :marked-for-removal conj new)
     (let [previous (-> acc :episodes peek)]
       (update acc :episodes conj
               (-> new
                   (period-number-and-episode-number previous)
                   (period-id)
                   (phase-number previous)
                   (phase-id)))))))

(defn add-periods-of-care [[id {::keys [ssda903-episodes] :as rec}]]
  [id
   (assoc rec ::ssda903-episodes
          (transduce
           (x/sort-by ::report-date)
           add-periods-of-care-rf
           ssda903-episodes))])

(def add-periods-of-care-xf
  (map add-periods-of-care))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; FIXME: We need to decide if we need logic to exclude incomplete
;; periods or not.
;;
;; Exclude periods of care that do not have a beginning episode where
;; RNE = S
(defn period-has-valid-start-episode? [period-of-care]
  (= (-> period-of-care first ::reason-new-episode) "S"))

(defn mark-incomplete-periods-of-care [[id {::keys [ssda903-episodes] :as rec}]]
  (let [removed-episodes (into [] (filter tagged-for-removal?) ssda903-episodes)]
    [id
     (assoc rec ::ssda903-episodes
            (into removed-episodes
                  (comp
                   (remove tagged-for-removal?)
                   (x/sort-by (juxt ::period-number ::report-date))
                   (partition-by ::period-number)
                   (mapcat (fn [period-of-care]
                             (if (period-has-valid-start-episode? period-of-care)
                               period-of-care
                               (into []
                                     (map (fn [episode]
                                            (update episode
                                                    ::edit
                                                    (fnil conj [])
                                                    {::command :examine
                                                     ::reason "Period did not start with an episode with a RNE of S"})))
                                     period-of-care)))))
                  ssda903-episodes))]))

(def mark-incomplete-periods-of-care-xf
  (map mark-incomplete-periods-of-care))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mark periods that start before the minimum report year
(defn period-starts-before-min-report-year? [min-report-year period-of-care]
  (t/<
   (-> period-of-care first ::report-date)
   (t/new-date min-report-year 4 1)))

(defn mark-periods-that-start-before-min-report-year [min-report-year [id {::keys [ssda903-episodes] :as rec}]]
  (let [removed-episodes (into [] (filter tagged-for-removal?) ssda903-episodes)]
    [id
     (assoc rec ::ssda903-episodes
            (into removed-episodes
                  (comp
                   (remove tagged-for-removal?)
                   (x/sort-by (juxt ::period-number ::report-date))
                   (partition-by ::period-number)
                   (mapcat (fn [period-of-care]
                             (if (and
                                  (not (period-has-valid-start-episode? period-of-care))
                                  (period-starts-before-min-report-year? min-report-year period-of-care))
                               (into []
                                     (map (fn [episode]
                                            (update episode
                                                    ::edit
                                                    (fnil conj [])
                                                    {::command :examine
                                                     ::reason (format "Period started before the minimum report year of %s with a non-S episode." min-report-year)})))
                                     period-of-care)
                               period-of-care))))
                  ssda903-episodes))]))

(defn mark-periods-that-start-before-min-report-year-xf [min-report-year]
  (map (partial mark-periods-that-start-before-min-report-year min-report-year)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fix episodes that take place after the extract date

(defn episode-starts-after-extract-date? [extract-date date]
  (if (nil? date)
    false
    (t/>
     date
     extract-date)))

(defn mark-episodes-that-cease-after-extract-date [extract-date {::keys [ceased] :as episode}]
  (if (episode-starts-after-extract-date? extract-date ceased)
    (-> episode
        (update ::edit
                (fnil conj [])
                {::command :updated
                 ::reason  (format "Ceased date takes place after extract date: %s" extract-date)
                 ::desciption (format "Changing ceased date from %s to nil" ceased)})
        (assoc ::ceased nil))

    episode))

(defn remove-episodes-that-start-after-extract-date [extract-date {::keys [report-date] :as episode}]
  (if (episode-starts-after-extract-date? extract-date report-date)
    (-> episode
        (update ::edit
                (fnil conj [])
                {::command :remove
                 ::reason "Episode starts after extract date"
                 ::desciption (format "Report date %s starts after extract date" report-date)}))
    episode))

(defn fix-episodes-where-dates-are-after-extract-date [extract-date]
  (comp
   (map (partial mark-episodes-that-cease-after-extract-date extract-date))
   (map (partial remove-episodes-that-start-after-extract-date extract-date))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Client data extraction -> episodes
(defn client-data-extraction->episodes-xf [{::keys [uasc-ids
                                                    max-report-year
                                                    min-report-year]
                                            :or {uasc-ids #{}}
                                            :as config}
                                           extract-date
                                           client-data-extraction-xf]
  (comp

   ;; client specific data extraction
   client-data-extraction-xf

   ;; mark single episodes
   (mark-uasc-for-removal-xf uasc-ids)
   mark-missing-required-episode-fields-xf
   mark-missing-required-header-fields-xf
   mark-short-term-break-legal-status-episodes-xf
   mark-fostering-as-updated-xf
   mark-bad-episode-interval-xf
   add-episode-interval-xf

   ;; Group by ID and process each group
   headers-and-episodes-by-id-xf

   ;; add the header
   create-header-xf
   add-header-fields-to-episodes-xf

   ;; Handle bad episode dates
   mark-stale-episodes-xf
   mark-fixed-missing-placement-episodes-xf
   mark-overlapping-episodes-for-id-xf
   mark-episode-overlapped-by-open-episodes-xf
   (mark-stale-history-xf max-report-year)
   mark-ordered-disjoint-episodes-xf
   (fix-episodes-where-dates-are-after-extract-date extract-date)

   ;; Handle periods/phases/episode numbers
   add-periods-of-care-xf
   (mark-periods-that-start-before-min-report-year-xf min-report-year)
   mark-incomplete-periods-of-care-xf

   ;; Things that want a clean list of episodes
   separate-episodes-for-removal-xf ;; make the clean list

   ))
