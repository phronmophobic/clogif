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
  "Creates a `media` by calling `drawf` with a Graphics2D and each element of `coll`. `drawf` should draw the current frame. The return value is ignored.

  The following options are available:
  `:fps` frames per second. default: 24.
  `:width` width of the media. default: 100.
  `:height` height of the media. default 100.

  Example:

  (graphics->media
   (fn [^Graphics2D g frameno]
     (.setColor g Color/white)
     (.fillRect g 0 0 100 100)
     (.setColor g Color/black)
     (.drawString g \"Hello World\" 5 50))
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
  `:dither` A dithering algorithm. One of  \"bayer\", \"heckbert\", \"floyd_steinberg\", \"sierra2\", \"sierra2_4a\", \"sierra3\", \"burkes\", \"atkinson\", \"none\". See avfilter/paletteuse for more info. default \"sierra2_4a\".
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

