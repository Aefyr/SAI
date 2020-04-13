## Overview
Since version 3.10, SAI is adding some meta information to .apks files it exports. This artice describes the format of that meta information.

Meta information consists of 2 files - `icon.png` and `meta.sai_v1.json`, they're located in the root directory of a .apks file (which is simply a renamed ZIP archive)

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
