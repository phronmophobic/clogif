(ns com.phronemophobic.code2gif
  (:require [membrane.java2d :as java2d]
            [membrane.ui :as ui]
            [membrane.components.code-editor.code-editor :as code-editor]
            [com.phronemophobic.clj-media.avfilter :as avfilter]
            [com.phronemophobic.clogif :as gif]
            [liq.buffer :as buffer]
            [liq.highlighter :as highlighter]))


(defn- insert-text [buf s]
  (-> buf
      (buffer/insert-string )
      (highlighter/highlight code-editor/hl)))

(defn- buf-view [buf]
  (code-editor/->Buffer nil true
                        buf))

(defn code->views [code-str]
  (->> (reductions (fn [{:keys [buf]} c]
                     {:buf (-> buf
                               (buffer/insert-char c)
                               (highlighter/highlight code-editor/hl))
                      :c c})
                   {:buf (buffer/buffer "" {})}
                   code-str)
       (remove (fn [{:keys [c]}]
                 (= c \space)))
       (map :buf)
       (map buf-view)
       (map #(ui/padding 4 %))))



(comment

  (def code
    "(def s
  \"Announcing clogif v1.3! Create gifs in clojure!\")

(def media
  (graphics->media
   java2d/draw-to-graphics
   {:width width
    :height 20}
   (eduction
    (map (fn [i]
           (subs s 0 i)))
    (map (fn [s]
           [(ui/filled-rectangle [1 1 1]
                                 width height)
            (ui/label s)]))
    (range (inc (count s))))))

(save-gif!
 (avfilter/vstack
  media
  code-media)
 \"announcement.gif\")"
    )

  (def views
    (into []
          (comp (map-indexed vector)
                (keep (fn [[i view]]
                        (when (even? i)
                          view))))
          (code->views code)))
  (def width (-> (apply max (map ui/width views))
                 int inc))
  (def height (-> (apply max (map ui/height views))
                  int inc))
  (def code-media
    (gif/graphics->media
     (fn [g view]
       (java2d/draw-to-graphics
        g
        [(ui/filled-rectangle [1 1 1]
                              width height)
         view]))
     
     {:width width
      :height height}
     (concat
      views
      ;; pause on the last frame for 4 seconds
      (repeat (* 24 4)
              (last views)))))

  (def s
    "Announcing clogif v1.3! Create gifs in clojure!")

  (def media
    (gif/graphics->media
     java2d/draw-to-graphics
     {:width width
      :height 20}
     (eduction
      (map (fn [i]
             (subs s 0 i)))
      (map (fn [s]
             [(ui/filled-rectangle [1 1 1]
                                   width height)
              (ui/label s)]))
      (range (inc (count s))))))

  (gif/save-gif!
   (avfilter/vstack
    media
    code-media)
   "announcement.gif")




  ,)


