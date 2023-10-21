# clogif

Create gifs in clojure!

Built with [FFmpeg](https://ffmpeg.org/) and [clj-media](https://github.com/phronmophobic/clj-media)

## Rationale

This library is a small wrapper around `clj-media` that offers a simplified API for creating GIFs using best practices.

## Dependency

```edn
com.phronemophobic/clogif {:mvn/version "1.0"}
```

## Usage

```clojure
(require '[com.phronemophobic.clogif :as gif])
```

```clojure
(save-gif!
 (graphics->media
  (fn [g frameno]
    (.setColor ^Graphics2D g Color/white)
    (.fillRect ^Graphics2D g 0 0 100 100)
    (.setColor ^Graphics2D g Color/black)
    (.drawString ^Graphics2D g (str "Hello World " frameno) 5 50)))
 "hello-world.gif")
```

### [Membrane](https://github.com/phronmophobic/membrane) Compatible!

```clojure
(require '[membrane.java2d :as java2d]
         '[membrane.ui :as ui])
(save-gif!
 (graphics->media
  (fn [g frameno]
    (java2d/draw-to-graphics
     g
     [(ui/filled-rectangle [1 1 1]
                           100 18)
      (ui/label (str "membrane: " frameno))]))
  {:width 100
   :height 18})
 "membrane.gif")
```

## License

Copyright © 2023 Adrian

The contents of this repository may be distributed under the Apache License v2.0 or the GPLv2.
