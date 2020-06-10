(ns scrabulator.text-processing
  (:require [clojure.string :as string]
            [scrabulator.scoring]))

(defn- letters-missing [letters-freq word-freq]
  (reduce (fn [result [letter word-letter-freq]]
            (let [avail-count (- (letters-freq letter) word-letter-freq)
                  aac         (Math/abs avail-count)]
              (if (< avail-count 0)
                (-> result
                    (update :missing-count + aac)
                    (update :missing-letters merge {letter aac}))
                result)))
          {:missing-count 0
           :missing-letters {}}
          word-freq))

(comment
  (letters-missing {} {})
  (letters-missing {\a 1 \b 2 \d 3 \q 1} {\b 1 \a 1 \d 1})
  (letters-missing {\a 1 \b 2 \d 3 \q 1} {\t 1 \a 1 \k 1 \e 1}))

(defn matching-words
  "List all words in the dictionary using only the letters and which match the pattern."
  [dictionary available-letters pattern]
  (let [avail-letter-freq (-> available-letters string/upper-case frequencies)
        blank-count (or (avail-letter-freq \_) 0)
        regex       (re-pattern (str "(?i)" (or pattern "")))
        matcher     (if (= pattern "")
                      (constantly true)
                      (partial re-find regex))]
    (reduce (fn [results {:keys [word letter-freq base-score] :as word-map}]
              (let [{:keys [missing-count missing-letters]} (letters-missing avail-letter-freq letter-freq)]
                (if (and (<= missing-count blank-count) (matcher word))
                  (->> (assoc word-map :missing-letters missing-letters
                                       :score           (- base-score (scrabulator.scoring/letter-freq->score missing-letters)))
                       (conj results))
                  results)))
            []
            dictionary)))