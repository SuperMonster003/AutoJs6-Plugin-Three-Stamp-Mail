<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="{{ repo_url }}/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="{{ repo_url }}/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="{{ icon_alt }}" border="0" width="128" />
    </picture>
  </p>

  <p>{{ text_plugin_synopsis }}</p>

  <p>
    <a href="{{ repo_url }}/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/{{ repo_slug }}?label=Release"/></a>
    <a href="{{ repo_url }}/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/{{ repo_slug }}?color=A24232&label=Issues"/></a>
    <a href="{{ license_url }}"><img alt="GitHub License" src="https://img.shields.io/github/license/{{ repo_slug }}?color=534BAE&label=License"/></a>
  </p>
</div>

******

### {{ h3_languages_with_ascii }}

******

{{ p_languages_all_supported_for_readme }}:

{{ placeholder_ul_languages_all_supported }}

******

### {{ h3_introduction }}

******

{{ p_introduction_what }}

{{ p_introduction_how }}

******

### {{ h3_status }}

******

{{ p_status }}

******

### {{ h3_features }}

******

{{ p_features_intro }}:

{{ placeholder_features }}

******

### {{ h3_usage }}

******

{{ placeholder_usage_steps }}

******

### {{ h3_providers }}

******

{{ p_providers_intro }}:

{{ placeholder_provider_steps }}

{{ p_providers_more }}

******

### {{ h3_quick_start }}

******

{{ p_quick_start_intro }}:

```js
// A saved alias keeps the credential inside the plugin; an inline account works as well:
// mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' })
let client = mail.connect('work');

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
watch.on('error', e => console.warn(e.code, e.message));

// Every network method also has an Async form; every failure is a MailError with a code.
mail.setDefault(client);
mail.searchAsync({ subject: 'invoice', since: '2026-09-01' }).then(list => console.log(list.length, list.fallback));
```

******

### {{ h3_aliases }}

******

{{ p_aliases_what }}

{{ p_aliases_how }}

******

### {{ h3_compatibility }}

******

{{ p_compatibility_intro }}:

{{ placeholder_compatibility_points }}

******

### {{ h3_faq }}

******

{{ placeholder_faq_items }}

******

### {{ h3_security }}

******

{{ p_security_intro }}

{{ placeholder_security_points }}

{{ p_security_permission }}

******

### {{ h3_plugin_interface }}

******

{{ p_plugin_interface }}:

```text
application id: {{ plugin_application_id }}
plugin id: {{ plugin_id }}
engine: {{ plugin_engine }}
variant: {{ plugin_variant }}
service action: {{ plugin_service_action }}
service category: {{ plugin_service_category }}
info action: {{ plugin_info_action }}
aidl interface: {{ plugin_aidl_interface }}
minimum host build: {{ required_host_version_code }} ({{ required_host_version_name }})
```

{{ p_contract_service }}

******

### {{ h3_roadmap }}

******

{{ p_roadmap }}

- [{{ text_link_roadmap }}]({{ roadmap_url }})

******

### {{ h3_release_history }}

******

{{ placeholder_latest_release_history }}

##### {{ h5_for_more_release_history }}

* {{ placeholder_read_more_in_changelog_md }}

******

### {{ h3_build }}

******

{{ p_build_intro }}

{{ p_build_debug }}:

```powershell
.\gradlew.bat :app:assembleDebug
```

{{ p_build_test }}:

```powershell
.\gradlew.bat :mail-core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

{{ p_build_release }}:

```powershell
.\gradlew.bat :app:assembleRelease
```

{{ p_build_digest }}:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

{{ p_build_docs_check }}:

```powershell
py .python\generate_markdown.py --check
```

{{ p_build_requirements }}

******

### {{ h3_resource_layout }}

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

{{ p_resource_layout }}

******

### {{ h3_license }}

******

{{ p_license }}

******

### {{ h3_links }}

******

- {{ text_link_autojs6 }}: {{ autojs6_url }}
- {{ text_link_autojs6_docs }}: {{ docs_autojs6_url }}
- {{ text_link_mail_docs }}: {{ docs_mail_url }}
- {{ text_link_angus_mail }}: {{ angus_mail_url }}
- {{ text_link_third_party_notices }}: {{ third_party_notices_url }}
