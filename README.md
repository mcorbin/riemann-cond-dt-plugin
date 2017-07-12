# riemann-cond-dt-plugin

Riemann plugin for checking if a condition is true for a time period.

## Howto

### Loading the plugin

First, you should download the .jar file, and tell Riemann how to find it in your system using the `/etc/default/riemann` or `/etc/sysconfig/riemann` file and the EXTRA_CLASSPATH option.

Then, on your `riemann.config` file, add:

```clojure
(load-plugins)

(require '[riemann-cond-dt.core :as cd])
```

You are now ready to use the plugin !

### Streams

#### above
Takes a number `threshold` and a time period in seconds `dt`.

If the condition `(> (:metric event) threshold)` is valid for all events received during at least the period `dt`, valid events received after the `dt` period will be passed on until an invalid event arrives.

`:metric` should not be nil (it will produce exceptions).

```clojure
(cd/above 1000 60 #(info %))
```

#### below

Takes a number `threshold` and a time period in seconds `dt`.

If the condition `(< (:metric event) threshold)` is valid for all events received during at least the period `dt`, valid events received after the `dt` period will be passed on until an invalid event arrives.

`:metric` should not be nil (it will produce exceptions).


```clojure
(cd/below 1000 60 #(info %))
```

##### between

Takes two numbers, `low` and `high`, and a time period in seconds, `dt`.

If the condition `(and (> (:metric event) low) (< (:metric event) high))` is valid for all events received during at least the period `dt`, valid events received after the `dt` period will be passed on until an invalid event arrives.

`:metric` should not be nil (it will produce exceptions)."

```clojure
(cd/between 1000 10000 60 #(info %))
```

#### outside

Takes two numbers, `low` and `high`, and a time period in seconds, `dt`.

If the condition `(or (< (:metric event) low) (> (:metric event) high))` is valid for all events received during at least the period `dt`, valid events received after the `dt` period will be passed on until an invalid event arrives.

`:metric` should not be nil (it will produce exceptions).

```clojure
(cd/outside 1000 10000 60 #(info %))
```

#### critical

Takes a time period in seconds `dt`.

If all events received during at least the period `dt` have `:state` critical, new critical events received after the `dt` period will be passed on until an invalid event arrives.

```clojure
(cd/critical 60 #(info %))
```

#### cond-dt

A stream which detects if a condition `(f event)` is true during `dt` seconds.
Takes a function of events `f` and a time period `dt` in seconds.

If the condition is valid for all events received during at least the period `dt`, valid events received after the `dt` period will be passed on until an invalid event arrives.

Skips events that are too old or that do not have a timestamp.

For example, you can reimplement `critical` using `cond-dt`:

```clojure
(cd/cond-dt (fn [event] (= (:state event) "critical")) 60 #(info %))
```
