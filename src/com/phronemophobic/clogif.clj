(ns com.phronemophobic.clogif
  (:require [com.phronemophobic.clj-media :as clj-media]
            [com.phronemophobic.clj-media.avfilter :as avfilter])
  (:import java.awt.image.BufferedImage
           java.awt.Graphics2D
           java.awt.Color
           java.awt.RenderingHints
           javax.imageio.ImageIO))

(defn ^:private new-img
  "Returns a new BufferedImage in a format suitable for creating a gif"
  [width height]
  (BufferedImage. width height BufferedImage/TYPE_4BYTE_ABGR))

(defn graphics->media
  "Creates a `media` by calling `drawf` with Graphics2D and each element of `coll`.

  The following options are available:
  `:fps` frames per second. default: 24.
  `:width` width of the media. default: 100.
  `:height` height of the media. default 100.

  Example:

  (graphics->media
   (fn [g frameno]
     (.setColor ^Graphics2D g Color/white)
     (.fillRect ^Graphics2D g 0 0 100 100)
     (.setColor ^Graphics2D g Color/black)
     (.drawString ^Graphics2D g \"Hello World\" 5 50))
  (range 24))"
  ([drawf coll]
   (graphics->media drawf {} coll))
  ([drawf
    {:keys [fps
            width
            height]
     :as opts}
    coll]
   (let [width (or width 100)
         height (or height 100)
         fps (or fps 24)
         img ^BufferedImage (new-img width height)
         frame-format
         (clj-media/video-format
          {:pixel-format :pixel-format/abgr
           :time-base fps
           :line-size (* width
                         ;; abgr has 4 bytes per pixel when packed
                         4)
           :width width
           :height height})

         g (.createGraphics img)
         frames
         (sequence
          (map-indexed
           (fn [pts x]
             (.setBackground g (Color. (int 255)
                                       (int 255)
                                       (int 255)
                                       (int 0)))
             (.clearRect g 0 0
                         width height)
             (.setColor ^Graphics2D g (Color/BLACK))
             (drawf g x)
             (clj-media/make-frame
              {:bytes (-> img
                          (.getData)
                          ^java.awt.image.DataBufferByte
                          (.getDataBuffer)
                          (.getData))
               :format frame-format
               :time-base (:time-base frame-format)
               :pts pts})))
          coll)]
     (clj-media/make-media frame-format
                           frames))))

(defn save-gif!
  "Write a gif to `fname` with contents of `media`.

  `media` can be any valid media.

  Available options:
  `:dither` A dithering algorithm. One of  \"bayer\", \"heckbert\", \"floyd_steinberg\", \"sierra2\", \"sierra2_4a\", \"sierra3\", \"burkes\", \"atkinson\", \"none\". See avfilter/paletteuse for more info. default \"sierra2_4a\"
  `:transparent?` Allow transparency. default: true.
  `:alpha-threshold` Alpha cutoff for transparency. default 128."
  ([media fname]
   (save-gif! media fname {}))
  ([media fname {:keys [dither
                        transparent?
                        alpha-threshold] :as opts}]
   (let [;; loop? (get opts :loop? true)
         alpha-threshold (or alpha-threshold
                             128)
         transparent? (get opts :transparent? true)
         palette (avfilter/palettegen
                  {:reserve-transparent transparent?}
                  media)
         media (avfilter/paletteuse
                (merge
                 {:avfilter/pixel-format :pixel-format/pal8}
                 (when alpha-threshold
                   {:alpha-threshold alpha-threshold})
                 (when dither
                   {:dither dither}))
                media palette)]
     (clj-media/write!
      media
      fname
      {:video-format
       {:codec {:name "gif",
                :id 97,}
        :pixel-format :pixel-format/pal8}}))))

(comment

  (do
    (require '[membrane.java2d :as java2d])
    (require '[membrane.ui :as ui])
    ,)

  (save-gif!
   (graphics->media
    (fn [g pts]
      (java2d/draw-to-graphics g
                               [(ui/filled-rectangle [1 1 1]
                                                     200 200)
                                (ui/translate
                                 pts pts
                                 (ui/label (quot pts 10)))
                                ]))
    {:fps 24}
    (range (* 24 5)))
   "test.gif"
   #_{:alpha-threshold 250}

   )


  ,)


(require 'dev)
(dev/add-libs '{mogenslund/liquid {:mvn/version "2.0.3"}})
  (do
    (require '[membrane.java2d :as java2d])
    (require '[membrane.ui :as ui])
    ,)
(require '[membrane.components.code-editor.code-editor :as code-editor]
         '[liq.buffer :as buffer]
         '[liq.highlighter :as highlighter])

(defn- insert-text [buf s]
  (-> buf
      (buffer/insert-string )
      (highlighter/highlight code-editor/hl)
      ))

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



(def code
  "(def s
  \"Announcing clogif v1.2! Create gifs in clojure!\")

(def media
  (graphics->media
   (fn [g s]
     (java2d/draw-to-graphics
      g
      [(ui/filled-rectangle [1 1 1]
                            width height)
       (ui/label s)]))
   {:width width
    :height 20}
   (concat
    (eduction
     (map (fn [i]
            (subs s 0 i)))
     (range (count s))))))

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
  (graphics->media
   (fn [g view]
     (java2d/draw-to-graphics
      g
      [(ui/filled-rectangle [1 1 1]
                            width height)
       view]))
   
   {:width (-> (apply max (map ui/width views))
               int inc)
    :height (-> (apply max (map ui/height views))
                int inc)}
   (concat
    views
    (repeat (* 24 4)
            (last views)))))

(def s
  "Announcing clogif v1.2! Create gifs in clojure!")

(def media
  (graphics->media
   (fn [g s]
     (java2d/draw-to-graphics
      g
      [(ui/filled-rectangle [1 1 1]
                            width height)
       (ui/label s)]))
   {:width width
    :height 20}
   (concat
    (eduction
     (map (fn [i]
            (subs s 0 i)))
     (range (count s))))))

(save-gif!
 (avfilter/vstack
  media
  code-media)
 "announcement.gif")


(def announcement-views
  (into []

        (map (fn [frameno]
               (let [len (quot frameno frames-per-char)]
                 [(ui/filled-rectangle [1 1 1]
                                       width height)
                  (ui/label (subs s 0 len))])))))

(clj-media/write! (avfilter/concat m2 m2) "foo.mp4")

(clj-media/write! (->> m2
                       (avfilter/edgedetect)
                       (avfilter/inflate)
                       (avfilter/gblur)
                       ) "foo.mp4")



(clj-media/write!
 (avfilter/concat
  {}
  (clj-media/file "code.gif")
  (avfilter/edgedetect (clj-media/file "code.gif"))
  )
 "foo.mp4")
