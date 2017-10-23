package com.bicou.sbt.hbs

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

import com.bicou.sbt.hbs.Import.HbsKeys
import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.web._
import sbt.Keys._
import sbt._
import spray.json._

object Import {

  object HbsKeys {
    val hbs = TaskKey[Seq[File]]("hbs", "Precompile handlebar templates.")
    val remfile = TaskKey[Unit]("remfile", "Removes old generated file")
    val finishfile = TaskKey[Unit]("finishfile", "Finishes generated file")

    val amd = SettingKey[Boolean]("hbs-amd", "Exports amd style (require.js)")
    val commonjs = SettingKey[String]("hbs-commonjs", "Exports CommonJS style, path to Handlebars module")
    val handlebarPath = SettingKey[String]("hbs-handlebarPath", "Path to handlebar.js (only valid for amd-style)")
    val known = SettingKey[Seq[String]]("hbs-known", "Known helpers")
    val knownOnly = SettingKey[Boolean]("hbs-knownOnly", "Known helpers only")
    val namespace = SettingKey[String]("hbs-namespace", "Template namespace")
    val root = SettingKey[String]("hbs-root", "Template root (base value that will be stripped from template names)")
    val data = SettingKey[Boolean]("hbs-data", "Include data when compiling")
    val bom = SettingKey[Boolean]("hbs-bom", "Removes the BOM (Byte Order Mark) from the beginning of the templates")
    val simple = SettingKey[Boolean]("hbs-simple", "Output template function only")
    val map = SettingKey[Boolean]("hbs-map", "Generates source maps")
  }

}

object SbtHbs extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtJsTask.autoImport.JsTaskKeys._
  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport.HbsKeys._

  val hbsUnscopedSettings = Seq(

    excludeFilter := HiddenFileFilter,
    includeFilter := "*.hbs" || "*.handlebars",

    jsOptions := JsObject(
      "amd" -> JsBoolean(amd.value),
      "commonjs" -> JsString(commonjs.value),
      "handlebarPath" -> JsString(handlebarPath.value),
      "known" -> JsArray(known.value.toList.map(JsString(_))),
      "knownOnly" -> JsBoolean(knownOnly.value),
      "namespace" -> JsString(namespace.value),
      "root" -> JsString(root.value),
      "data" -> JsBoolean(data.value),
      "bom" -> JsBoolean(bom.value),
      "simple" -> JsBoolean(simple.value),
      "map" -> JsBoolean(map.value)
    ).toString()
  )

  override def projectSettings = Seq(
    remfile := {
        streams.value.log.info("Handlebars removing old generated files...")

        val f = new File((WebKeys.webTarget.value / "hbs" / "main" / "templates.js").toURI)

        f.delete()

        f.getParentFile().mkdirs()

        f.createNewFile()

      if (amd.value) {
        Files.write(Paths.get((WebKeys.webTarget.value / "hbs" / "main" / "templates.js").toURI), "define(['handlebars.runtime'], function(Handlebars) {\n  Handlebars = Handlebars[\"default\"];\n var template = Handlebars.template, templates = window.JST = window.JST || {};\nHandlebars.partials = templates;\n".getBytes(StandardCharsets.UTF_8))
      }
    },

    finishfile := {
      if (amd.value) {
        streams.value.log.info("Handlebars finishing generated file.")

        Files.write(Paths.get((WebKeys.webTarget.value / "hbs" / "main" / "templates.js").toURI), "\nreturn templates;\n});".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND)
      }
    },

    finishfile <<= finishfile triggeredBy (HbsKeys.hbs),
    finishfile in Assets <<= finishfile in Assets triggeredBy (HbsKeys.hbs in Assets),
    finishfile in TestAssets <<= finishfile in TestAssets triggeredBy (HbsKeys.hbs in TestAssets),

    amd := false,
    commonjs := "",
    handlebarPath := "",
    known := Seq(),
    knownOnly := false,
    namespace := "",
    root := "",
    data := false,
    bom := false,
    simple := false,
    map := false

  ) ++ inTask(hbs)(
    SbtJsTask.jsTaskSpecificUnscopedSettings ++
      inConfig(Assets)(hbsUnscopedSettings) ++
      inConfig(TestAssets)(hbsUnscopedSettings) ++
      Seq(
        moduleName := "hbs",
        shellFile := getClass.getClassLoader.getResource("handlebars-shell.js"),

        taskMessage in Assets := "Handlebars compiling",
        taskMessage in TestAssets := "Handlebars test compiling"
      )
  ) ++ SbtJsTask.addJsSourceFileTasks(hbs) ++ Seq(
    hbs in Assets := (hbs in Assets).dependsOn(nodeModules in Assets).value,
    hbs in TestAssets := (hbs in TestAssets).dependsOn(nodeModules in TestAssets).value,

    hbs in Assets := (hbs in Assets).dependsOn(remfile in Assets).value,
    hbs in TestAssets := (hbs in TestAssets).dependsOn(remfile in TestAssets).value
  )

}

// vim: set ts=2 sw=2 et:
