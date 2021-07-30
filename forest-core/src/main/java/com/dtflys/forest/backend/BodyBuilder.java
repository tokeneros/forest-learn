package com.dtflys.forest.backend;

import com.dtflys.forest.handler.LifeCycleHandler;
import com.dtflys.forest.http.ForestRequest;

/**
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2017-05-19 14:50
 */
public interface BodyBuilder<R> {

    void buildBody(R req, ForestRequest request, LifeCycleHandler lifeCycleHandler);
}
