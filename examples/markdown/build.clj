(ns build-markdown-example
  (:require [e-paper.storage :as ep])
  (:use [e-paper.authoring :only (script)])
  (:use [e-paper.execution :only (run-calclet)]))

; Create an empty e-paper
(def paper (ep/create (java.io.File. "markdown_example.h5")))

; Add references to the jar files from two libraries used by the calclets
(def jars (ep/store-library-references paper "markdown"))

; Store input parameters for calclets
(ep/create-text paper "overview" "markdown"
"Markdown example
================

The purpose of this simple example is to illustrate how
text written using simple markup languages can be integrated
into an e-paper.

Advantages
----------

Simple markup langugages such as Markdown are

 - easy to use and thus

 - fast to learn.
")

(ep/store-program paper "show-overview" jars "renderer" ["/text/overview"])

; Close the e-paper file.
(ep/close paper)
