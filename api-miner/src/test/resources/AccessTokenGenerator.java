/**
 * Copyright (c) 2010 Enric Ruiz, Ignasi Barrera
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.rhymestore.twitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.RequestToken;

/**
 * Interactive method to generates the OAuth {@link AccessToken}.
 * 
 * @author Ignasi Barrera
 * @see Twitter
 * @see OAuthAuthorization
 */
public class AccessTokenGenerator
{
    /**
     * Interactive {@link AccessToken} generation.
     * 
     * @param args No args are required.
     * @throws Exception If the token cannot be generated.
     */
    public static void main(final String... args) throws Exception
    {
        Twitter twitter = new TwitterFactory().getInstance();
        RequestToken requestToken = twitter.getOAuthRequestToken();

        // Ask for the PIN
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        AccessToken accessToken = null;

        System.out.print("Open the following URL and grant access to your account: ");
        System.out.println(requestToken.getAuthorizationURL());
        System.out.print("Enter the PIN: ");

        accessToken = twitter.getOAuthAccessToken(requestToken, br.readLine());

        System.out.println("AccessToken Key: " + accessToken.getToken());
        System.out.println("AccessToken Secret: " + accessToken.getTokenSecret());

        twitter.shutdown();
    }
}
