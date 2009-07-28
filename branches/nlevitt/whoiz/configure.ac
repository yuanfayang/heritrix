AC_INIT([Whoiz],[0.0.1],[mailto:nlevitt@gmail.com],[whoiz])

AM_INIT_AUTOMAKE([1.9 foreign dist-bzip2])

AC_CONFIG_SRCDIR([whoiz.c])
AC_CONFIG_HEADERS([config.h])

AM_MAINTAINER_MODE

AC_PROG_CC
AC_ISC_POSIX
AC_HEADER_STDC
AC_PROG_LIBTOOL

# ***************************
# Check for required packages
# ***************************

GLIB_REQUIRED=2.18.1

PKG_CHECK_MODULES([GLIB], [glib-2.0 >= $GLIB_REQUIRED])

# *************************************
# *************************************

AC_SUBST([AM_CPPFLAGS])
AC_SUBST([AM_CFLAGS])
AC_SUBST([AM_CXXFLAGS])
AC_SUBST([AM_LDFLAGS])

AC_CONFIG_FILES([Makefile])

AC_OUTPUT
