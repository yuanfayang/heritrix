# $Id: Makefile.am 6425 2009-08-01 19:24:25Z nlevitt $

bin_PROGRAMS = bin_search

bin_search_SOURCES = \
	bin_search.c	

bin_search_CPPFLAGS = \
	-I$(top_srcdir)		\
	$(DISABLE_DEPRECATED)	\
	$(AM_CPPFLAGS)

bin_search_CFLAGS = \
	$(DEPENDENCIES_CFLAGS)	\
	$(WARN_CFLAGS)		\
	$(AM_CFLAGS)

bin_search_LDFLAGS = \
	$(AM_LDFLAGS)

bin_search_LDADD = \
	$(DEPENDENCIES_LIBS)	\
 	$(INTLLIBS)

EXTRA_DIST = autogen.sh
