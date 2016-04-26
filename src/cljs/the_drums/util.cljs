(ns the-drums.util
  (:require [cljs.core.async :refer [chan timeout close! <! >! put! sliding-buffer]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn debounce-chan
  "Returns a chan upon which will be put one (the most recent) value from in-ch
   *after* the gap between puts has exceeded delay-ms."
  [in-ch delay-ms]
  (let [out-ch (chan)]
    (go-loop [last-msg nil
              delayed-chan (chan)]
      (alt!
        in-ch ([msg]
               (if msg
                 (recur msg (timeout delay-ms))
                 (close! out-ch)))

        delayed-chan ([_]
                      (>! out-ch last-msg)
                      (recur nil (chan)))

        :priority true))

    out-ch))
