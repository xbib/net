package org.xbib.net.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.xbib.net.path.simple.Path;
import org.xbib.net.path.simple.PathComparator;
import org.xbib.net.path.simple.PathMatcher;

@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@Fork(value = 1, jvmArgsAppend = "-Xmx1024m")
@Threads(4)
@Timeout(time = 10, timeUnit = TimeUnit.MINUTES)
public class SimplePathBenchmark {

	@State(Scope.Benchmark)
	public static class AllRoutesPatternParser extends PatternParserData {

		@Setup(Level.Trial)
		public void registerPatterns() {
			parseRoutes(RouteGenerator.allRoutes());
		}
	}

	@State(Scope.Benchmark)
	public static class StaticRoutesPatternParser extends PatternParserData {

		@Setup(Level.Trial)
		public void registerPatterns() {
			parseRoutes(RouteGenerator.staticRoutes());
		}
	}

	@State(Scope.Benchmark)
	public static class AllRoutesAntPathMatcher extends PathMatcherData {

		@Setup(Level.Trial)
		public void registerPatterns() {
			parseRoutes(RouteGenerator.allRoutes());
		}
	}

	@Benchmark
	public void matchAllRoutesWithAntPathMatcher(AllRoutesAntPathMatcher data, Blackhole bh) {
		for (String path : data.requestPaths) {
			for (String pattern : data.patterns) {
				bh.consume(data.matcher.match(pattern, path));
			}
		}
	}

	@Benchmark
	public void matchAndSortAllRoutesWithAntPathMatcher(AllRoutesAntPathMatcher data, Blackhole bh) {
		for (String path : data.requestPaths) {
			List<Path> matches = new ArrayList<>();
			for (String pattern : data.patterns) {
				if (data.matcher.match(pattern, path)) {
					matches.add(new Path(pattern));
				}
			}
			matches.sort(new PathComparator(path));
			bh.consume(matches);
		}
	}

	@State(Scope.Benchmark)
	public static class StaticRoutesAntPathMatcher extends PathMatcherData {

		@Setup(Level.Trial)
		public void registerPatterns() {
			parseRoutes(RouteGenerator.staticRoutes());
		}
	}

	@Benchmark
	public void matchStaticRoutesWithAntPathMatcher(StaticRoutesAntPathMatcher data, Blackhole bh) {
		for (String path : data.requestPaths) {
			for (String pattern : data.patterns) {
				bh.consume(data.matcher.match(pattern, path));
			}
		}
	}

    static class PatternParserData {

        List<String> patterns = new ArrayList<>();

        List<String> requestPaths = new ArrayList<>();

        void parseRoutes(List<Route> routes) {
            routes.forEach(route -> {
            	this.patterns.add(route.pattern);
            	this.requestPaths.addAll(route.matchingPaths);
            });
        }

    }

	static class PathMatcherData {

		PathMatcher matcher = new PathMatcher();

		List<String> patterns = new ArrayList<>();

		List<String> requestPaths = new ArrayList<>();

		void parseRoutes(List<Route> routes) {
			routes.forEach(route -> {
				this.patterns.add(route.pattern);
				this.requestPaths.addAll(route.matchingPaths);
			});
		}

	}

	/**
	 * Route in the web application.
	 * Each route has a path pattern and can generate sets of matching request paths for that pattern.
	 */
	static class Route {

		private final String pattern;

		private final List<String> matchingPaths;

		public Route(String pattern, String... matchingPaths) {
			this.pattern = pattern;
			if (matchingPaths.length > 0) {
				this.matchingPaths = Arrays.asList(matchingPaths);
			}
			else {
				this.matchingPaths = Collections.singletonList(pattern);
			}
		}

		public String pattern() {
			return this.pattern;
		}

		public Iterable<String> matchingPaths() {
			return this.matchingPaths;
		}
	}

	static class RouteGenerator {

		static List<Route> staticRoutes() {
			return Arrays.asList(
					new Route("/"),
					new Route("/why-spring"),
					new Route("/microservices"),
					new Route("/reactive"),
					new Route("/event-driven"),
					new Route("/cloud"),
					new Route("/web-applications"),
					new Route("/serverless"),
					new Route("/batch"),
					new Route("/community/overview"),
					new Route("/community/team"),
					new Route("/community/events"),
					new Route("/community/support"),
					new Route("/some/other/section"),
					new Route("/blog.atom")
			);
		}

		static List<Route> captureRoutes() {
			return Arrays.asList(
					new Route("/guides"),
					new Route("/guides/gs/{repositoryName}",
							"/guides/gs/rest-service", "/guides/gs/scheduling-tasks",
							"/guides/gs/consuming-rest", "/guides/gs/relational-data-access"),
					new Route("/projects"),
					new Route("/projects/{name}",
							"/projects/spring-boot", "/projects/spring-framework",
							"/projects/spring-data", "/projects/spring-security", "/projects/spring-cloud"),
					new Route("/blog/category/{category}.atom",
							"/blog/category/releases.atom", "/blog/category/engineering.atom",
							"/blog/category/news.atom"),
					new Route("/tools/{name}", "/tools/eclipse", "/tools/vscode"),
					new Route("/team/{username}",
							"/team/jhoeller", "/team/bclozel", "/team/snicoll", "/team/sdeleuze", "/team/rstoyanchev"),
					new Route("/api/projects/{projectId}",
							"/api/projects/spring-boot", "/api/projects/spring-framework",
							"/api/projects/reactor", "/api/projects/spring-data",
							"/api/projects/spring-restdocs", "/api/projects/spring-batch"),
					new Route("/api/projects/{projectId}/releases/{version}",
							"/api/projects/spring-boot/releases/2.3.0", "/api/projects/spring-framework/releases/5.3.0",
							"/api/projects/spring-boot/releases/2.2.0", "/api/projects/spring-framework/releases/5.2.0")
			);
		}

		static List<Route> regexRoute() {
			return Arrays.asList(
					new Route("/blog/{year:\\\\d+}/{month:\\\\d+}/{day:\\\\d+}/{slug}",
							"/blog/2020/01/01/spring-boot-released", "/blog/2020/02/10/this-week-in-spring",
							"/blog/2020/03/12/spring-one-conference-2020", "/blog/2020/05/17/spring-io-barcelona-2020",
							"/blog/2020/05/17/spring-io-barcelona-2020", "/blog/2020/06/06/spring-cloud-release"),
					new Route("/user/{name:[a-z]+}",
							"/user/emily", "/user/example", "/user/spring")
			);
		}

		static List<Route> allRoutes() {
			List<Route> routes = new ArrayList<>();
			routes.addAll(staticRoutes());
			routes.addAll(captureRoutes());
			routes.addAll(regexRoute());
			routes.add(new Route("/static/**", "/static/image.png", "/static/style.css"));
			routes.add(new Route("/**", "/notfound", "/favicon.ico"));
			return routes;
		}

	}
}
