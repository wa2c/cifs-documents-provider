CIFS Documents Provider
=======================

## About

**CIFS Documents Provider** is an Android app to provide access to shared online storage.

<div style="display: flex">
<img width="240" alt="Home Screen" src="./fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" />
<img width="240" alt="Home Screen" src="./fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" /> 
<img width="240" alt="Home Screen" src="./fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" />
</div>

## App Store

* [Google Play](https://play.google.com/store/apps/details?id=com.wa2c.android.cifsdocumentsprovider)
* [F-Droid](https://f-droid.org/packages/com.wa2c.android.cifsdocumentsprovider/)
* [Amazon Appstore](https://www.amazon.com/gp/product/B09D4264PB) 

## Release History (APK Download)

* [Release](https://github.com/wa2c/cifs-documents-provider/releases)

## Source Code

* [GitHub](https://github.com/wa2c/cifs-documents-provider)

## Module Structure

```mermaid
graph TD

subgraph "app"
  app_module[app]
end

subgraph "presetntation"
  presetntation_module[presetntation]
  app_module --> presetntation_module
end

subgraph "domain"
  domain_module[domain]
  presetntation_module --> domain_module
end
 
subgraph "data"
  subgraph "storage"
    data_storage_modules[jcifs, smbj, ...]
    data_storage_interfaces_module[interfaces]
    data_storage_modules --> data_storage_interfaces_module
    domain_module --> data_storage_interfaces_module
  end
  data_data_module[data]
  domain_module -->  data_data_module[data]
  data_data_module --> data_storage_interfaces_module
  data_data_module --> data_storage_modules
end

subgraph "common"
  common_module[common]
  app_module --> common_module
  presetntation_module --> common_module
  domain_module --> common_module
  data_data_module --> common_module
  data_storage_modules --> common_module
  data_storage_interfaces_module --> common_module
end
```

## Guide

* [Wiki](https://github.com/wa2c/cifs-documents-provider/wiki)

## Licence

Copyright &copy; 2020 wa2c [MIT License](https://github.com/wa2c/cifs-documents-provider/blob/main/LICENSE)

## Author

[wa2c](https://github.com/wa2c)
