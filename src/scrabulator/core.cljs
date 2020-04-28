(ns ^:figwheel-hooks scrabulator.core
  (:require
    [goog.dom :as gdom]
    [reagent.core :as reagent :refer [atom]]
    [ajax.core :refer [GET]]
    [clojure.string :as string]
    [scrabulator.text-processing]
    [scrabulator.scoring]))

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:dictionary   nil
                          :letters      ""
                          :regex        ""
                          :last-message ""
                          :ready?       false
                          :matches      []}))

(def init (do
            (println "loading the sowpods.txt dictionary.")
            (GET "/sowpods.txt" {:handler       (fn [response]
                                                  (let [r (->> response
                                                               js->clj
                                                               string/split-lines
                                                               (mapv (fn [word]
                                                                       (let [f (frequencies word)]
                                                                         {:word        word
                                                                          :letter-freq f
                                                                          :base-score  (scrabulator.scoring/letter-freq->score f)}))))]
                                                    (swap! app-state assoc :dictionary r :ready? true)
                                                    (println "loaded the dictionary successfully with scores.")))
                                 :error-handler (fn [{:keys [status status-text] :as response}]
                                                  (println "failed to load sowpods.txt.  Status" status ":" status-text))})))

(defn get-app-element []
  (gdom/getElement "app"))

(defn letters-container []
  [:div
   [:label {:for "letters"} "what letters do you have?"]
   [:input {:type "text"
            :value (:letters @app-state)
            :name "letters"
            :on-change #(swap! app-state assoc :letters (-> % .-target .-value))}]])

(defn regex-container []
  [:div
   [:label {:for "regex"} "would you like them to match a pattern?"]
   [:input {:type "text"
            :name "regex"
            :on-change #(swap! app-state assoc :regex (-> % .-target .-value))}]])

(defn calc-container []
  [:div
   [:input {:type "button"
            :value "calculate!"
            :disabled (-> @app-state :ready? not)
            :on-click #(do
                         (println (js/Date.) "calculating matches")
                         (let [{:keys [dictionary letters regex]} @app-state
                               results (scrabulator.text-processing/matching-words dictionary letters regex)]
                           (swap! app-state assoc :matches results))
                         (println (js/Date.) "done all matches"))}]])

(defn matched-word-container [{:keys [word score] :as word-map}]
  ^{:key (str "mwc-" word)} [:li {:key (str "mwc-" word)} (str word " (" score ")")])

(defn results-container []
  [:div
   "results: "
   [:ol
    (for [{:keys [word] :as word-map} (:matches @app-state)]
      ^{:key (str "mwc-" word)} [matched-word-container word-map])]])

(defn app-container []
  [:div
   [:h3 "Welcome to the scrabulator!"]
   [:div
    [letters-container]
    [regex-container]
    [calc-container]
    [results-container]]])

(defn mount [el]
  (reagent/render-component [app-container] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element))
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

