(ns build-markdown-example
  (:require [active-papers.storage :as ep])
  (:use [active-papers.authoring :only (script)])
  (:use [active-papers.execution :only (run-calclet)]))

; Create an empty active paper
(def paper (ep/create (java.io.File. "markdown_example.h5")))

; Add references to the jar files from two libraries used by the calclets
(def jars (ep/store-library-references paper "markdown"))

; Store input parameters for calclets
(ep/create-text paper "overview" "markdown"
"Markdown example
================

The purpose of this simple example is to illustrate how
text written using simple markup languages can be integrated
into an active paper.

Advantages
----------

Simple markup langugages such as Markdown are

 - easy to use and thus

 - fast to learn.
")

(ep/store-program paper "show-overview" jars "renderer" ["/text/overview"])

; Close the active paper file.
(ep/close paper)
