(ns frontend.components.build-timings
  (:require [om.core :as om :include-macros true]
            [frontend.models.build :as build])
  (:require-macros [frontend.utils :refer [html]]))

(defn timings-width []  (-> (.querySelector js/document ".build-timings")
                            (.-offsetWidth)))
(def bar-height 20)
(def bar-gap 10)
(def container-bar-height (- bar-height bar-gap))

;;; Helpers
(defn create-x-scale [start-time stop-time]
  (let [start-time (js/Date. start-time)
        stop-time  (js/Date. stop-time)]
    (-> (js/d3.time.scale)
        (.domain #js [start-time stop-time])
        (.range  #js [0 (timings-width)]))))

(defn create-root-svg [number-of-containers]
  (-> (.select js/d3 ".build-timings")
      (.select "svg")
      (.attr "width" (timings-width))
      (.attr "height" (* number-of-containers bar-height))))

(defn container-position [step]
  (* bar-height (inc (aget step "index"))))

(defn scaled-time [x-scale step time-key]
  (x-scale (js/Date. (aget step time-key))))

;;; Elements of the visualization
(defn draw-containers! [x-scale step]
  (let [step-length      #(- (scaled-time x-scale % "end_time")
                             (scaled-time x-scale % "start_time"))
        step-start-pos   #(x-scale (js/Date. (aget % "start_time")))]
    (-> step
        (.selectAll "rect")
          (.data #(aget % "actions"))
        (.enter)
          (.append "rect")
          (.attr "class"  "container-step")
          (.attr "width"  step-length)
          (.attr "height" container-bar-height)
          (.attr "y"      container-position)
          (.attr "x"      step-start-pos))))

(defn draw-step-start-line! [x-scale step]
  (let [step-start-position #(scaled-time x-scale % "start_time")]
  (-> step
      (.selectAll "line")
        (.data #(aget % "actions"))
      (.enter)
        (.append "line")
        (.attr "class" "container-step-start-line")
        (.attr "x1"    step-start-position)
        (.attr "x2"    step-start-position)
        (.attr "y1"    container-position)
        (.attr "y2"    #(+ (container-position %)
                           container-bar-height)))))

(defn draw-steps! [x-scale chart steps]
  (let [step (-> chart
                 (.selectAll "g")
                   (.data (clj->js steps))
                 (.enter)
                   (.append "g"))]
    (draw-step-start-line! x-scale step)
    (draw-containers! x-scale step)))

(defn draw-chart! [{:keys [parallel steps start_time stop_time] :as build}]
  (let [x-scale (create-x-scale start_time stop_time)
        chart   (create-root-svg parallel)]
    (draw-steps! x-scale chart steps)))

;;;; Main component
(defn build-timings [build owner]
  (reify
    om/IInitState
    (init-state [_]
      {:drawn? false})
    om/IDidMount
    (did-mount [_]
      (om/set-state! owner :drawn? true)
      (draw-chart! build))
    om/IDidUpdate
    (did-update [_ _ _]
      (when (om/get-state owner :drawn?)
        (draw-chart! build)))
    om/IRenderState
    (render-state [_ _]
      (html
       [:div.build-timings
        [:svg]]))))
