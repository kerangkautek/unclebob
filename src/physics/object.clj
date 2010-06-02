(ns physics.object
  (:refer-clojure :exclude (merge))
  (:use clojure.contrib.combinatorics)
  )

(require ['physics.position :as 'position])
(require ['physics.vector :as 'vector])

(defstruct object :position :mass :velocity :force :name)

(defn make
  ([]
    (struct object (position/make) 0 (vector/make) (vector/make) "TILT"))

  ([position mass velocity force name]
    (struct object position mass velocity force name))
  )

(defn gravity [m1 m2 r]
  (/ (* m1 m2) (* r r)))

(defn force-between [o1 o2]
  (let [
    p1 (:position o1)
    p2 (:position o2)
    d (position/distance p1 p2)
    uv (vector/unit (vector/subtract p2 p1))
    g (gravity (:mass o1) (:mass o2) d)
    ]
    (vector/scale uv g))
  )

(defn accumulate-forces
  ([o os]
    (assoc o :force (accumulate-forces o os (vector/make))))

  ([o os f]
    (cond
      (empty? os) f
      (= o (first os)) (recur o (rest os) f)
      :else (recur o (rest os) (vector/add f (force-between o (first os))))
      )
    )
  )

(defn calculate-forces-on-all [os]
  (loop [objects os ns []]
    (if (empty? objects)
      ns
      (let [
        o (first objects)
        n (accumulate-forces o os)
        ]
        (recur (rest objects) (conj ns n))
        )
      )
    )
  )

(defn accelerate [o]
  (let [
    f (:force o)
    m (:mass o)
    v (:velocity o)
    av (vector/add v (vector/scale f (/ 1.0 m)))
    ]
    (-> o (assoc :velocity av) (assoc :force (vector/make)))
    )
  )

(defn accelerate-all [os]
  (loop [os os ns []]
    (if (empty? os)
      ns
      (recur (rest os) (conj ns (accelerate (first os))))
      )
    )
  )

(defn reposition [o]
  (let [
    p (:position o)
    v (:velocity o)
    ]
    (assoc o :position (position/add p v))
    )
  )

(defn reposition-all [os]
  (loop [os os ns []]
    (if (empty? os)
      ns
      (recur (rest os) (conj ns (reposition (first os))))
      )
    )
  )

(defn collided? [o1 o2]
  (let [
    p1 (:position o1)
    p2 (:position o2)
    ]
    (>= 3 (position/distance p1 p2))
    )
  )

(defn merge [
  {n1 :name, p1 :position, m1 :mass, v1 :velocity f1 :force}
  {n2 :name, p2 :position, m2 :mass, v2 :velocity f2 :force}]
  (let [
    p (position/average p1 p2)
    m (+ m1 m2)
    mv1 (vector/scale v1 m1)
    mv2 (vector/scale v2 m2)
    v (vector/scale (vector/add mv1 mv2) (/ 1 m))
    f (vector/add f1 f2)
    n (if (> m1 m2) (str n1 "." n2) (str n2 "." n1))
    ]
    (make p m v f n)
    )
  )

(defn collide [o1 o2 os]
  (conj
    (->> os (remove #(= o1 %)) (remove #(= o2 %)))
    (merge o1 o2))
  )

(defn collide-all [os]
  (loop [pairs (combinations os 2) cos os]
    (if (empty? pairs)
      cos
      (let [
        [o1 o2] (first pairs)
        ]
        (if (collided? o1 o2)
          (recur (rest pairs) (collide o1 o2 cos))
          (recur (rest pairs) cos)
          )
        )
      )
    )
  )

(defn update-all [os]
  (-> os collide-all calculate-forces-on-all accelerate-all reposition-all))