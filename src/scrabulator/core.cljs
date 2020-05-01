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
                          :last-message "Loading app..."
                          :ready?       false
                          :matches      []}))

(defn seconds-since [js-date]
  (/ (- (js/Date.) js-date) 1000))

(defonce init (do
                (swap! app-state assoc :last-message "loading the dictionary of accepted words.")
                (let [start-time (js/Date.)]
                  (GET "/sowpods.txt" {:handler       (fn [response]
                                                        (let [r (->> response
                                                                     js->clj
                                                                     string/split-lines
                                                                     (mapv (fn [word]
                                                                             (let [f (frequencies word)]
                                                                               {:word        word
                                                                                :letter-freq f
                                                                                :base-score  (scrabulator.scoring/letter-freq->score f)}))))
                                                              msg      (str "dictionary loaded in " (seconds-since start-time) " seconds.")]
                                                          (swap! app-state assoc :dictionary r :ready? true :last-message msg)))
                                       :error-handler (fn [{:keys [status status-text] :as response}]
                                                        (swap! app-state assoc :last-message (str "failed to load the dictionary.  Status" status ":" status-text)))}))))

(defn get-app-element []
  (gdom/getElement "app"))

(defn letters-container []
  [:div.letter-container
   [:input {:type "text"
            :value (:letters @app-state)
            :name "letters"
            :placeholder "what tiles do you have?"
            :on-change #(swap! app-state assoc :letters (-> % .-target .-value))}]])

(defn regex-container []
  [:div
   [:input {:type "text"
            :name "regex"
            :placeholder "optional pattern?"
            :on-change #(swap! app-state assoc :regex (-> % .-target .-value))}]])

(defn calc-container []
  [:div
   [:input.pure-button.pure-button-primary.calc-button
    {:type "button"
     :value "go"
     :disabled (-> @app-state :ready? not)
     :on-click #(do
                  (swap! app-state assoc :last-message "calculating matches...") ; doesn't show because the event handler has the thread of control...
                  (let [start-time (js/Date.)
                        {:keys [dictionary letters regex]} @app-state
                        results (scrabulator.text-processing/matching-words dictionary letters regex)]
                    (swap! app-state assoc :matches results :last-message (str "found " (count results) " words in " (seconds-since start-time) " seconds."))))}]])

(defn matched-word-container [{:keys [word score] :as word-map}]
  [:div.pure-u-1-4 word])

(defn results-container []
  (when (< 0 (-> app-state deref :matches count))
    [:div
     (for [[score word-map-list] (->> app-state
                                      deref
                                      :matches
                                      (group-by :score)
                                      (into (sorted-map-by >)))]
       [:div.pure-g
        [:legend.pure-u-5-5 (str "Words that score " score)]
        (for [word-map word-map-list]
          ^{:key (str "mwc-" (:word word-map))} [matched-word-container word-map])])]))

(defn app-container []
  [:div.appcontainer
   [:h1 "Scrabulator"]
   [:div
    [:form.pure-form
     [:fieldset
      [letters-container]
      [regex-container]
      [calc-container]
      [results-container]]]]
   [:hr]
   "sirwobin's tinker toy.  you can find the cljs source on " [:a {:href "https://github.com/sirwobin/scrabulator.core"} "github"]
   [:br]
   [:div (:last-message @app-state)]])

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

