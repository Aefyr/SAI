## Overview
Since version 3.10, SAI is adding some meta information to .apks files it exports. This artice describes the format of that meta information.

Meta information consists of 3 files - `icon.png`, `meta.sai_v1.json` and `meta.sai_v2.json`, they're located in the root directory of a .apks file (which is simply a renamed ZIP archive)

## icon.png
`icon.png` file is simply the icon of an app in PNG format, image size depends on device DPI and it doesn't has to be some exact size

## meta.sai_v1.json
`meta.sai_v1.json` is a text file with a JSON that contains some meta information about the app. It's named that way so it can easily be recognized and possibly updated later to v2 or something.

The JSON schema of the file is as follows (this isn't [a literal JSON](https://json-schema.org) schema though, since those are huge and this format is super simple), all elements are contained in the root element of the JSON:

```
package string — package of the app

label string — name/label of the app

version_code long — version code of the app

version_name string — version name of the app

export_timestamp long — time in milliseconds since Unix epoch to the moment this export has occured
```

Here's an example of meta JSON, it's probably even easier to understand than the schema above:
```json
{
   "export_timestamp":1586784470720,
   "label":"GitHub",
   "package":"com.github.android",
   "version_code":73,
   "version_name":"1.1.0"
}
```

## meta.sai_v2.json
`meta.sai_v2.json` is a text file with a JSON that contains some meta information about the app. It's an updated version of the meta file that was introduced in SAI 4.0, it includes some additional information - if app is a split APK, min and target SDK version of the app and backup components array for managed backups.

The JSON schema of the file is as follows (this isn't [a literal JSON](https://json-schema.org) schema though, since those are huge and this format is super simple), all elements are contained in the root element of the JSON:

```
package string — package of the app

label string — name/label of the app

version_code long — version code of the app

version_name string — version name of the app

export_timestamp long — time in milliseconds since Unix epoch to the moment this export has occured

split_apk boolean - true if app is a split APK

meta_version long - version of this meta file

min_sdk long - minimal Android SDK version supported by the app

target_sdk long - target Android SDK version of the app

backup_components - array of backup component objects with the following structure:
    type string - type of this backup component

    size long - size in bytes of this backup components

    data optional string - optional data specific to this component
```

Here's an example of meta JSON, it's probably even easier to understand than the schema above:
```json
{
   "backup_components":[
      {
         "size":6223899,
         "type":"apk_files"
      }
   ],
   "export_timestamp":1591311108223,
   "split_apk":true,
   "label":"GitHub",
   "meta_version":2,
   "min_sdk":23,
   "package":"com.github.android",
   "target_sdk":29,
   "version_code":109,
   "version_name":"1.2.0"
}
```
