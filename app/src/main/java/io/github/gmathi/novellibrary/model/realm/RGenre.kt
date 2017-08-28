package io.github.gmathi.novellibrary.model.realm

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects


class RGenre: RealmObject() {

    @PrimaryKey
    var name: String? = null

    @LinkingObjects("genres")
    val owners: RealmResults<RNovel>? = null

}
