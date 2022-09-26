package org.prebid.server.proto.openrtb.ext.request.admaru;

//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor(staticName = "of")
@Value
public class ExtImpAdmaru {

    String adspaceid;
    String pubid;
}
