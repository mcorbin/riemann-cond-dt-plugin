(ns riemann-cond-dt.core
  (:require [riemann.streams :refer [call-rescue]]))

(defn cond-dt
  "A stream which detects if a condition `(f event)` is true during `dt` seconds.
  Takes a function of events `f` and a time period `dt` in seconds.

  If the condition is valid for all events received during at least the period `dt`, valid events received after the `dt` period will be passed on until an invalid event arrives.
  Skips events that are too old or that do not have a timestamp."
  [f dt & children]
  (let [last-changed-state (atom {:ok false
                                  :time nil})]
    (fn [event]
      (let [{ok :ok changed-state-time :time} @last-changed-state
            event-time (:time event)
            valid-event (f event)]
        (when event-time ;; filter events with no time
          (swap! last-changed-state (fn [state]
                        (cond
                          (and valid-event (and (not ok)
                                                (or (not changed-state-time)
                                                    (> event-time changed-state-time))))
                          ;; event is validating the condition
                          ;; last event is not ok, has no time or is too old
                          ;; => last-changed-state is now ok with a new time
                          {:ok true :time event-time}
                          (and (not valid-event) (and ok
                                                      (or (not changed-state-time)
                                                          (> event-time changed-state-time))))
                          ;; event is not validating the condition
                          ;; last event is ok, has no time or is too old
                          ;; => last-changed-state is now ko with a new time
                          {:ok false :time event-time}
                          ;; default value, return the state
                          :default state)))
          (when (and valid-event
                      ;; we already had an ok event
                     ok
                     ;; check is current time > first ok event + dt
                     (> event-time (+ changed-state-time dt)))
            (call-rescue event children)))))))

(defn above
  "Takes a number `threshold` and a time period in seconds `dt`.
  If the condition `(> (:metric event) threshold)` is valid for all events received during at least the period `dt`, valid events received after the `dt` period will be passed on until an invalid event arrives.
  `:metric` should not be nil (it will produce exceptions)."
  [threshold dt & children]
  (apply cond-dt (fn [event] (> (:metric event) threshold)) dt children))

(defn below
  "Takes a number `threshold` and a time period in seconds `dt`.
  If the condition `(< (:metric event) threshold)` is valid for all events received during at least the period `dt`, valid events received after the `dt` period will be passed on until an invalid event arrives.
  `:metric` should not be nil (it will produce exceptions)."
  [threshold dt & children]
  (apply cond-dt (fn [event] (< (:metric event) threshold)) dt children))

(defn between
  "Takes two numbers, `low` and `high`, and a time period in seconds, `dt`.
  If the condition `(and (> (:metric event) low) (< (:metric event) high))` is valid for all events received during at least the period `dt`, valid events received after the `dt` period will be passed on until an invalid event arrives.
  `:metric` should not be nil (it will produce exceptions)."
  [low high dt & children]
  (apply cond-dt (fn [event] (and (> (:metric event) low)
                                  (< (:metric event) high))) dt children))

(defn outside
  "Takes two numbers, `low` and `high`, and a time period in seconds, `dt`.
  If the condition `(or (< (:metric event) low) (> (:metric event) high))` is valid for all events received during at least the period `dt`, valid events received after the `dt` period will be passed on until an invalid event arrives.
  `:metric` should not be nil (it will produce exceptions)."
  [low high dt & children]
  (apply cond-dt (fn [event] (or (< (:metric event) low)
                                 (> (:metric event) high))) dt children))

(defn critical
  "Takes a time period in seconds `dt`.
  If all events received during at least the period `dt` have `:state` critical, new critical events received after the `dt` period will be passed on until an invalid event arrives."
  [dt & children]
  (apply cond-dt (fn [event] (= (:state event) "critical")) dt children))
