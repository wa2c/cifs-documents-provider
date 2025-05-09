CIFS Documents Provider
=======================

## About

**CIFS Documents Provider** is an Android app to provide access to shared online storage.

<div style="display: flex">
<img width="240" alt="Home Screen" src="./fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" />
<img width="240" alt="Home Screen" src="./fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" /> 
<img width="240" alt="Home Screen" src="./fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" />
</div>

## Download

### Google Play

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80" />](https://play.google.com/store/apps/details?id=com.wa2c.android.cifsdocumentsprovider)

### F-Droid

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80" />](https://f-droid.org/packages/com.wa2c.android.cifsdocumentsprovider/)

### Amazon Appstore

[<img src="https://images-na.ssl-images-amazon.com/images/G/01/mobile-apps/devportal2/res/images/amazon-appstore-badge-english-black.png" alt="Get it on Amazon Appstore" height="55">](https://www.amazon.com/gp/product/B09D4264PB)

### Github

[<img src="https://censorship.no/img/github-badge.png" alt="Get it on Github" height="80">](https://github.com/wa2c/cifs-documents-provider/releases)

## Release History (APK Download)

* [Release](https://github.com/wa2c/cifs-documents-provider/releases)

## Source Code

* [GitHub](https://github.com/wa2c/cifs-documents-provider)

## Module Structure

```mermaid
graph TD

subgraph "app package"
  app_module[app]
end

subgraph "presetntation package"
  presetntation_module[presetntation]

  app_module --> presetntation_module
end

subgraph "domain package"
  domain_module[domain]

  presetntation_module --> domain_module
end
 
subgraph "data package"
  subgraph "storage package"
    data_storage_manager[manager]
    data_storage_modules[jcifs, smbj, ...]
    data_storage_interfaces_module[interfaces]

    data_storage_manager --> data_storage_modules
    data_storage_manager --> data_storage_interfaces_module
    domain_module --> data_storage_manager
    domain_module --> data_storage_interfaces_module
  end

  subgraph "data package"
    data_data_module[data]

    data_storage_manager --> data_data_module
    domain_module --> data_data_module
  end

end

subgraph "common"
  common_module[common]

  app_module --> common_module
  presetntation_module --> common_module
  domain_module --> common_module
  data_data_module --> common_module
  data_storage_modules --> common_module
  data_storage_manager --> common_module
  data_storage_interfaces_module --> common_module
end
```

## Guide

* [Wiki](https://github.com/wa2c/cifs-documents-provider/wiki)

## Translation

If you need a translation into your language, please add the translation data on the next page.

* [CIFS Documents Provider Translation Sheet](https://docs.google.com/spreadsheets/d/1y71DyM31liwjcAUuPIk3CuIqxJD2l9Y2Q-YZ0I0XE_E/edit?gid=0#gid=0)

## Licence

Copyright &copy; 2020 wa2c [MIT License](https://github.com/wa2c/cifs-documents-provider/blob/main/LICENSE)

## Author

[wa2c](https://github.com/wa2c)
