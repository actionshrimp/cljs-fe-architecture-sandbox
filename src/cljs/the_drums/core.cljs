(ns the-drums.core
  (:require [cljs.core.async :as a]
            [cljs.core.match :refer-macros [match]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defonce !handlers (atom {}))
(defonce event-chan (a/chan))
(defonce !db (atom {}))
(defonce !render-fn (atom nil))

(defn dispatch! [e app-state]
  (doall
   (for [h (vals @!handlers)]
     (h e app-state))))

(defn send-event! [e]
  (dispatch! e @!db)
  (@@!render-fn @!db))

(defn register-handler! [id f]
  (swap! !handlers assoc id f))

(defn set-render-fn! [f]
  (reset! !render-fn f))

;;(defn start-event-loop! [!render-fn]
;;  ;;Probably much more 'proper'/'better' ways of doing this, I'm a core.async noob
;;  (@!render-fn @!db)
;;
;;  (loop [first-event (a/<! event-chan)]
;;
;;    (loop [e first-event]
;;      (dispatch! e @!db)
;;      (when-let [another-e (a/poll! event-chan)]
;;        (recur another-e)))
;;
;;    (@!render-fn @!db)
;;    (recur (a/<! event-chan))))
