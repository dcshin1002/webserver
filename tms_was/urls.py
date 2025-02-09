"""tms_was URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/2.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from django.urls import path

import tmscore.Adapter as adap

urlpatterns = [
    path('', adap.index, name='firebase_test'),
    path('download/<int:year>/<int:month>/<int:day>/', adap.downloadPage, name='clientAppDownload'),
    path('get/<int:year>/<int:month>/<int:day>/', adap.getClusters, name='getClusters'),
    path('set/<int:year>/<int:month>/<int:day>/', adap.setClusters, name='setClusters'),
    path('route/<int:year>/<int:month>/<int:day>/', adap.setRoute, name='setRoute'),
    path('job/<str:jobid>', adap.getWorkProgress, name='jobProgress'),
    path('admin/', admin.site.urls),
]
