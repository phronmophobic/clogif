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
  "Creates a `media` by calling `drawf` with Graphics2D and the frame number.

  The following options are available:
  `:fps` frames per second. default: 24.
  `:num-frames` number of frames in media. default: 24.
  `:width` width of the media. default: 100.
  `:height` height of the media. default 100.

  Example:

  (graphics->media
   (fn [g frameno]
     (.setColor ^Graphics2D g Color/white)
     (.fillRect ^Graphics2D g 0 0 100 100)
     (.setColor ^Graphics2D g Color/black)
     (.drawString ^Graphics2D g \"Hello World\" 5 50)))"
  ([drawf]
   (graphics->media drawf {}))
  ([drawf
    {:keys [fps
            num-frames
            width
            height]
     :as opts}]
   (let [width (or width 100)
         height (or height 100)
         num-frames (or num-frames 24)
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
         (into []
               (map (fn [pts]
                      (.setBackground g (Color. (int 255)
                                                (int 255)
                                                (int 255)
                                                (int 0)))
                      (.clearRect g 0 0
                                  width height)
                      (.setColor ^Graphics2D g (Color/BLACK))
                      (drawf g pts)
                      (clj-media/make-frame
                       {:bytes (-> img
                                   (.getData)
                                   ^java.awt.image.DataBufferByte
                                   (.getDataBuffer)
                                   (.getData))
                        :key-frame? true
                        :format frame-format
                        :time-base (:time-base frame-format)
                        :pts pts})))
               (range num-frames))]
     (clj-media/make-media frame-format
                           frames))))

(defn save-gif!
  ([media fname]
   (save-gif! media fname {}))
  ([media fname {:keys [dither
                        transparent?
                        alpha-threshold] :as opts}
    ]
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
    {:num-frames (* 24 5)
     :fps 24}
    )
   "test.gif"
   #_{:alpha-threshold 250}

   )

  (save-gif!
   (graphics->media
   (fn [g frameno]
     (.setColor ^Graphics2D g Color/white)
     (.fillRect ^Graphics2D g 0 0 100 100)
     (.setColor ^Graphics2D g Color/black)
     (.drawString ^Graphics2D g "Hello World" 5 50)))
   "example.gif")


  ,)



