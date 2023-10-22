# clogif

Create gifs in clojure!

Built with [FFmpeg](https://ffmpeg.org/) and [clj-media](https://github.com/phronmophobic/clj-media).

## Rationale

This library is a small wrapper around `clj-media` that offers a simplified API for creating GIFs using best practices.

## Dependency

```edn
com.phronemophobic/clogif {:mvn/version "1.1"}
```

## Usage

```clojure
(require '[com.phronemophobic.clogif :as gif])
```

```clojure
(gif/save-gif!
 (gif/graphics->media
  (fn [^Graphics2D g frameno]
    (.setColor g Color/white)
    (.fillRect g 0 0 100 100)
    (.setColor g Color/black)
    (.drawString g (str "Hello World " frameno) 5 50)))
 "hello-world.gif")
```
![Hello World](/assets/hello-world.gif?raw=true)

### [Membrane](https://github.com/phronmophobic/membrane) Compatible!

```clojure
(require '[membrane.java2d :as java2d]
         '[membrane.ui :as ui])
(gif/save-gif!
 (gif/graphics->media
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

![Membrane gif](/assets/membrane.gif?raw=true)

## License

Copyright © 2023 Adrian

The contents of this repository may be distributed under the Apache License v2.0 or the GPLv2.
