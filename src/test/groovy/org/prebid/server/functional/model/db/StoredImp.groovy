package org.prebid.server.functional.model.db

import groovy.transform.ToString
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table
import org.prebid.server.functional.model.db.typeconverter.ImpConfigTypeConverter
import org.prebid.server.functional.model.request.auction.BidRequest
import org.prebid.server.functional.model.request.auction.Imp

import static javax.persistence.GenerationType.IDENTITY

@Entity
@Table(name = "stored_imps")
@ToString(includeNames = true)
class StoredImp {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id")
    Integer id
    @Column(name = "accountId")
    String accountId
    @Column(name = "impId")
    String impId
    @Column(name = "impData")
    @Convert(converter = ImpConfigTypeConverter)
    Imp impData

    static StoredImp getStoredImp(BidRequest bidRequest) {
        getStoredImp(bidRequest.accountId, bidRequest.imp[0])
    }

    static StoredImp getStoredImp(String accountId, Imp impression) {
        def impressionId = impression.ext?.prebid?.storedRequest?.id ?: impression.id
        getStoredImp(accountId, impressionId, impression)
    }

    static StoredImp getStoredImp(String accountId, String impressionId, Imp impression) {
        new StoredImp().tap {
            it.accountId = accountId
            it.impId = impressionId
            it.impData = impression
        }
    }
}
