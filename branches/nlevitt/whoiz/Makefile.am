
bin_PROGRAMS = whoiz

whoiz_SOURCES = \
	whoiz.c	

whoiz_CPPFLAGS = \
	-I$(top_srcdir)					\
	$(DISABLE_DEPRECATED)				\
	$(AM_CPPFLAGS)

whoiz_CFLAGS = \
	   $(GLIB_CFLAGS)	\
	   $(WARN_CFLAGS)		\
	   $(AM_CFLAGS)

whoiz_LDFLAGS = \
	$(AM_LDFLAGS)

whoiz_LDADD = \
	$(GLIB_LIBS)	\
 	$(INTLLIBS)

EXTRA_DIST = autogen.sh
