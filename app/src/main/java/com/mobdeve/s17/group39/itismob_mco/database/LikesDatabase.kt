package com.mobdeve.s17.group39.itismob_mco.database

import com.mobdeve.s17.group39.itismob_mco.models.ListModel

object LikesDatabase : DatabaseHandler<ListModel>(FirestoreDatabase.listsCollection) {

}