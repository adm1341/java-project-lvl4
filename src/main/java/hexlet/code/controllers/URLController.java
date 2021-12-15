package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class URLController {
    public static Handler listURLs = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pagedURLS = new QUrl()
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedURLS.getList();

        int lastPage = pagedURLS.getTotalPageCount() + 1;
        int currentPage = pagedURLS.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());



        ctx.attribute("urls", urls);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/index.html");
    };
    public static Handler createUrl = ctx -> {
        String urlString = ctx.formParam("url");

        URL urlObject;
        try {
            urlObject = new URL(urlString);
        } catch (java.net.MalformedURLException urlException) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.attribute("url", urlString);
            ctx.render("index.html");
            return;
        }
        String normalUrlString;
        if (urlObject.getPort() == -1) {
            normalUrlString = urlObject.getProtocol() + "://" + urlObject.getHost();
        } else {
            normalUrlString = urlObject.getProtocol() + "://" + urlObject.getHost() + ":" + urlObject.getPort();
        }


        Url urlSearch = new QUrl()
                .name.equalTo(normalUrlString)
                .findOne();

        if (urlSearch != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.attribute("url", urlString);
            ctx.render("index.html");
            return;
        }
        Url url = new Url(normalUrlString);
        url.save();

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.render("index.html");
    };
    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        List<UrlCheck> urlChecks = new QUrlCheck()
                .url.equalTo(url)
                .orderBy().id.desc()
                .findList();

        ctx.attribute("urlChecks", urlChecks);

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };
    public static Handler checkUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        HttpResponse<String> response;

        try {
            response = Unirest.get(url.getName()).asString();

            int statusCode = response.getStatus();

            Document body = Jsoup.parse(response.getBody());

            String title = body.title();

            String description = null;

            if (body.selectFirst("meta[name=description]") != null) {
                description = body.selectFirst("meta[name=description]").attr("content");
            }

            String h1 = null;

            if (body.selectFirst("h1") != null) {
                h1 = body.selectFirst("h1").text();
            }


            UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
            urlCheck.save();

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Страница недоступна");
            ctx.sessionAttribute("flash-type", "danger");
        }
        ctx.redirect("/urls/" + id);
    };
}

