package civerooni.com.putman.ui.parts;

import civerooni.com.putman.core.model.ResponseModel;

class ResponseReplay {
    final ResponseModel response;
    final int format;

    ResponseReplay(ResponseModel response, int format) {
        this.response = response;
        this.format = format;
    }
}
