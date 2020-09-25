// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.grid.security;

import org.openqa.selenium.internal.Require;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.Filter;
import org.openqa.selenium.remote.http.HttpHandler;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import java.util.Objects;
import java.util.logging.Logger;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.openqa.selenium.grid.security.AddSecretFilter.HEADER_NAME;

public class RequiresSecretFilter implements Filter {

  private static final Logger LOG = Logger.getLogger(RequiresSecretFilter.class.getName());
  private final Secret secret;

  public RequiresSecretFilter(Secret secret) {
    this.secret = secret;
  }

  @Override
  public HttpHandler apply(HttpHandler httpHandler) {
    Require.nonNull("HTTP handler", httpHandler);

    return req -> {
      if (!isSecretMatch(secret, req)) {
        return new HttpResponse()
          .setStatus(HTTP_UNAUTHORIZED)
          .addHeader("Content-Type", "text/plain; charset=UTF-8")
          .setContent(Contents.utf8String("Unauthorized access attempted to " + req.getUri()));
      }

      return httpHandler.execute(req);
    };
  }

  private boolean isSecretMatch(Secret secret, HttpRequest request) {
    String header = request.getHeader(HEADER_NAME);
    if (header == null) {
      if (secret != null) {
        LOG.warning("Unexpected secret sent!");
        return false;
      }
      return true;
    }

    Secret requestSecret = new Secret(header);

    if (!Objects.equals(secret, requestSecret)) {
      LOG.warning("Secrets did not match!");
      return false;
    }

    return true;
  }
}
