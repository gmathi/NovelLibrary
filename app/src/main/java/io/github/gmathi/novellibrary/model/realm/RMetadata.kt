package io.github.gmathi.novellibrary.model.realm

import io.realm.RealmObject


class RMetadata: RealmObject() {

    var key: String = ""
    var value: String? = null

}