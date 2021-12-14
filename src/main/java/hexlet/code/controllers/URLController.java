package hexlet.code.controllers;

import hexlet.code.domain.Url;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import hexlet.code.domain.query.QUrl;

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

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };
}
