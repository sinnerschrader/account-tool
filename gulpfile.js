var gulp = require('gulp');

var config = {
	source: 'src/main/resources/public/',
	target: 'target/classes/public/',
	finalName: 'account-tool',
	failAfterError: true
};

var handleError = function (err) {
	console.error(err);
};

gulp.task('lint:js', () => {
	var eslint = require('gulp-eslint');
	var noop = require('gulp-noop');
	return gulp.src([
			config.source + 'extensions/js/**/*.js',
			config.source + 'extensions/js/**/*.json',
			'!node_modules/**'
		])
		.pipe(eslint())
		.pipe(eslint.format())
		.pipe(config.failAfterError ? eslint.failAfterError() : noop());
});

gulp.task("build:js", ['lint:js'], function () {
	var babelify = require('babelify');
	var browserify = require('browserify');
	var buffer = require('vinyl-buffer');
	var source = require('vinyl-source-stream');
	var sourcemaps = require('gulp-sourcemaps');
	var uglify = require('gulp-uglify');
	var rename = require('gulp-rename');

	var bundler = browserify({
		entries: config.source + 'extensions/js/index.js',
		debug: true
	});
	bundler.transform(babelify);
	bundler.bundle()
		.on('error', handleError)
		.pipe(source(config.finalName + '.js'))
		.pipe(buffer())
		.pipe(gulp.dest(config.target + 'extensions/'))
		.pipe(rename({suffix: '.min'}))
		.pipe(sourcemaps.init({loadMaps: true}))
		.pipe(uglify())
		.pipe(sourcemaps.write('./'))
		.pipe(gulp.dest(config.target + 'extensions/'));
});

gulp.task('build:css', function () {
	var sass = require('gulp-sass');
	var sourcemaps = require('gulp-sourcemaps');
	var autoprefixer = require('gulp-autoprefixer');
	var cssnano = require('gulp-cssnano');
	var rename = require('gulp-rename');

	return gulp.src(config.source + 'extensions/css/index.scss')
		.pipe(rename(config.finalName + '.css'))
		.pipe(sourcemaps.init())
		.pipe(sass({style: 'expanded'}).on('error', sass.logError))
		.pipe(autoprefixer('last 2 version'))
		.pipe(gulp.dest(config.target + 'extensions/'))
		.pipe(rename({suffix: '.min'}))
		.pipe(cssnano().on('error', handleError))
		.pipe(sourcemaps.write('.'))
		.pipe(gulp.dest(config.target + 'extensions/'));
});

gulp.task('build:copy-resources', function () {
	var copyResources = [config.source + '**', '!' + config.source + '**/*.svg'];
	gulp.src(copyResources).pipe(gulp.dest(config.target));
});

gulp.task('watch', function () {
	gulp.watch(config.source + '**', ['build:copy-resources']);
	gulp.watch(config.source + 'extensions/js/**/*.js', ['build:js']);
	gulp.watch(config.source + 'extensions/css/**/*.scss', ['build:css']);
});

gulp.task('build', ['build:js', 'build:css']);

gulp.task('development', function () {
	config.failAfterError = false;
	gulp.start(['build', 'watch']);
});

gulp.task('default', ['dev']);
