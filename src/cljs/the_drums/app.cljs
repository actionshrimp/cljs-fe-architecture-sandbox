(ns the-drums.app
  (:require [reagent.core :as r]
            [cljs.core.async :as a]
            [cljs.core.match :refer-macros [match]]
            [the-drums.core :as c]
            [the-drums.user-page :as u]
            [devtools.core :as devtools])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go go-loop]]))

(defn render-root [app-state]
  (js/console.log app-state)
  (r/render-component (u/user-page app-state)
                      (.getElementById js/document "container")))

;; So render root doesnt get trapped in a closure and we can reload it.
(defonce !render-fn (atom render-root))
(reset! !render-fn render-root)

(def initial-state {:page-state {}})

(defn init []
  (enable-console-print!)
  (devtools/install! [:custom-formatters :sanity-hints])

  (c/set-render-fn! !render-fn)
  (c/register-handler! :event-log #(js/console.log "Event received: " %))
  (reset! c/!db initial-state)

  ;;(c/start-event-loop! !render-fn)
  ;;(set! (.-onerror js/window)
  ;;      (fn [e]
  ;;        (js/window.setTimeout
  ;;         (fn []
  ;;           (println "Restarting event loop after error.")
  ;;           (c/start-event-loop! !render-fn))
  ;;         1000)))

  (c/send-event! [:navigate :user-page]))

(defn reload! []
  (enable-console-print!)
  (render-root @c/!db))

(comment
  (do
    (reset! c/!db initial-state)
    (reload!))

  (a/put! c/event-chan []))
