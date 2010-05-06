/*
 * Copyright (C) 2010  Sylvain Maucourt (smaucourt@gmail.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 *
 */
package net.sylvek.sharemyposition.client;

import com.capsula.gwt.reversegeocoder.client.ExtendedPlacemark;
import com.capsula.gwt.reversegeocoder.client.ReverseGeocoder;
import com.capsula.gwt.reversegeocoder.client.ReverseGeocoderCallback;
import com.google.code.gwt.geolocation.client.Coordinates;
import com.google.code.gwt.geolocation.client.Geolocation;
import com.google.code.gwt.geolocation.client.Position;
import com.google.code.gwt.geolocation.client.PositionCallback;
import com.google.code.gwt.geolocation.client.PositionError;
import com.google.code.gwt.geolocation.client.PositionOptions;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

public class Client implements EntryPoint {

    final Geolocation geo = Geolocation.getGeolocation();

    final static String HOST = "http://sharemyposition.appspot.com/";

    final static String URL_STATIC = HOST + "static.jsp";

    private Button location;

    @Override
    public void onModuleLoad()
    {
        location = new Button("click here to find my location");
        location.setStylePrimaryName("button");
        location.setEnabled(Geolocation.isSupported());
        location.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event)
            {
                location.setEnabled(false);
                location.setText("location is requested ...");
                requestPosition();
            }
        });
        RootPanel.get("geolocalisation").add(location);

        if (!Geolocation.isSupported()) {
            RootPanel.get("error")
                    .add(
                            new Label(
                                    "Geolocation API is not supported. You need to use a iPhone, Android Phone or Firefox browser (> 3.5)."));
        }

    }

    private void requestPosition()
    {
        geo.getCurrentPosition(new PositionCallback() {
            public void onFailure(PositionError error)
            {
                String message = "";
                switch (error.getCode()) {
                case PositionError.UNKNOWN_ERROR:
                    message = "Unknown Error";
                    break;
                case PositionError.PERMISSION_DENIED:
                    message = "Permission Denied";
                    break;
                case PositionError.POSITION_UNAVAILABLE:
                    message = "Position Unavailable";
                    break;
                case PositionError.TIMEOUT:
                    message = "Time-out";
                    break;
                default:
                    message = "Unknown error code.";
                }
                RootPanel.get("error").add(
                        new Label("Message: '" + error.getMessage() + "', code: " + error.getCode() + " (" + message + ")"));
            }

            public void onSuccess(Position position)
            {
                Coordinates c = position.getCoords();

                final LatLng point = LatLng.newInstance(c.getLatitude(), c.getLongitude());
                final String url = URL_STATIC + "?pos=" + point.toUrlValue();
                ReverseGeocoder.reverseGeocode(point, new ReverseGeocoderCallback() {

                    @Override
                    public void onFailure(LatLng point)
                    {
                        getShortyUrl(url, new RequestCallback() {
                            public void onError(Request request, Throwable exception)
                            {
                                addLinks(url, "");
                            }

                            public void onResponseReceived(Request request, Response response)
                            {
                                if (200 == response.getStatusCode()) {
                                    addLinks(response.getText(), "");
                                }
                            }
                        });
                    }

                    @Override
                    public void onSuccess(final ExtendedPlacemark placemark)
                    {
                        getShortyUrl(url, new RequestCallback() {
                            public void onError(Request request, Throwable exception)
                            {
                                addLinks(url, "");
                            }

                            public void onResponseReceived(Request request, Response response)
                            {
                                if (200 == response.getStatusCode()) {
                                    addLinks(response.getText(), placemark.getAddress());
                                }
                            }
                        });
                    }

                });
            }
        }, PositionOptions.getPositionOptions(true /* GPS if possible */, 60000 /* timeout 1min */, 0 /* new position */));
    }

    private void addLinks(String url, String address)
    {
        location.setVisible(false);

        RootPanel.get("links")
                .add(
                        new HTML("<a class='tinylink' href='" + "mailto:?subject=my%20current%20position&body=i%20am%20here,%20"
                                + url + " " + URL.encode(address)
                                + "'>share my position by mail</a><br/>or copy this link and send it by sms"));

        RootPanel.get("tinyurl").add(new HTML("<a class='tinylink' href='" + url + "'>" + url + "</a>"));
    }

    private String getUrlService()
    {
        StringBuffer url = new StringBuffer(GWT.getModuleBaseURL());
        int pos = url.indexOf(GWT.getModuleName());
        return url.subSequence(0, pos).toString();
    }

    private void getShortyUrl(String url, RequestCallback requestCallback)
    {
        String query = getUrlService() + "service/create?url=" + url;
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(query));
        try {
            builder.sendRequest(null, requestCallback);
        } catch (RequestException e) {
            Window.alert(e.getMessage());
        }
    }

}
