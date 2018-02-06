var gulp = require('gulp');

var config = {
	source: 'src/main/resources/public/',
	target: 'build/resources/main/public/',
	finalName: 'account-tool',
	failAfterError: true
}

var handleError = function(err) {
	console.error(err);
}

gulp.task("build:js", function () {
	var babelify = require('babelify');
	var browserify = require('browserify');
  var source = require('vinyl-source-stream')
	var rename = require('gulp-rename');

	var bundler = browserify({
		entries: config.source + 'extensions/js/index.js',
		debug: true
	});
	bundler.transform(babelify);
	bundler.bundle()
		.on('error', handleError)
		.pipe(source(config.finalName + '.js'))
		.pipe(gulp.dest(config.target + 'extensions/'))
		.pipe(rename({suffix: '.min'}))
		.pipe(gulp.dest(config.target + 'extensions/'));
});

gulp.task('build:css', function() {
	var sass = require('gulp-sass');
	var rename = require('gulp-rename');

	return gulp.src(config.source + 'extensions/css/index.scss')
		.pipe(rename(config.finalName + '.css'))
	 	.pipe(sass({ style: 'expanded' }).on('error', sass.logError))
		.pipe(gulp.dest(config.target + 'extensions/'))
		.pipe(rename({suffix: '.min'}))
		.pipe(gulp.dest(config.target + 'extensions/'));
});

gulp.task('build:copy-resources', function() {
	var copyResources = [config.source + '**', '!'+ config.source + '**/*.svg'];
	gulp.src(copyResources ).pipe(gulp.dest(config.target));
});

gulp.task('build', ['build:js', 'build:css']);
