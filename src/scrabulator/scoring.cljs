(ns scrabulator.scoring)

(def points->letters {1 [\E \A \I \O \U \N \R \T \L \S]
                      2 [\D \G]
                      3 [\B \C \M \P]
                      4 [\F \H \V \W \Y]
                      5 [\K]
                      8 [\J \X]
                      10 [\Q \Z]})

(def letter->score (->> points->letters
                        (map (fn [[score letters]]
                               (interleave letters (repeat score))))
                        flatten
                        (partition 2)
                        (into {})))

(defn letter-freq->score [freq]
  (reduce (fn [score [letter letter-freq]]
            (+ score (-> letter letter->score (* letter-freq))))
          0
          freq))

(comment
  (letter-freq->score {\B 1 \N 1 \D 2})
  (letter-freq->score {"L" 1, "S" 1, "E" 1, "R" 1, "V" 1, "O" 1, "A" 2, "W" 1, "D" 1}))